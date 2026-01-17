package Library;

import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LibrarySystem extends JFrame {


    private static final String URL = "jdbc:mysql://localhost:3306/library_project";
    private static final String USER = "root";
    private static final String PASSWORD = "Sujith@123"; 


    public static Connection connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "DB Error: " + e.getMessage());
            return null;
        }
    }

    public static void loadBooks(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = connect()) {
            if (conn == null) return;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM books");
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("title"), rs.getString("author"), rs.getString("genre"), rs.getInt("quantity")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void addBook(String title, String author, String genre, int qty) {
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO books (title, author, genre, quantity) VALUES (?, ?, ?, ?)")) {
            if (conn == null) return;
            pstmt.setString(1, title); pstmt.setString(2, author); pstmt.setString(3, genre); pstmt.setInt(4, qty);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "Book Added!");
        } catch (SQLException e) { JOptionPane.showMessageDialog(null, "Error: " + e.getMessage()); }
    }

    public static void loadMembers(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = connect()) {
            if (conn == null) return;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM members");
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("phone")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void addMember(String name, String email, String phone) {
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO members (name, email, phone) VALUES (?, ?, ?)")) {
            if (conn == null) return;
            pstmt.setString(1, name); pstmt.setString(2, email); pstmt.setString(3, phone);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "Member Added!");
        } catch (SQLException e) { JOptionPane.showMessageDialog(null, "Error: " + e.getMessage()); }
    }

    public static void issueBook(int bookId, int memberId) {
        try (Connection conn = connect()) {
            if (conn == null) return;
            
            PreparedStatement check = conn.prepareStatement("SELECT quantity FROM books WHERE id=?");
            check.setInt(1, bookId);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt("quantity") > 0) {
                PreparedStatement trans = conn.prepareStatement("INSERT INTO transactions (book_id, member_id, issue_date, status) VALUES (?, ?, CURDATE(), 'ISSUED')");
                trans.setInt(1, bookId); trans.setInt(2, memberId);
                trans.executeUpdate();

                PreparedStatement update = conn.prepareStatement("UPDATE books SET quantity = quantity - 1 WHERE id=?");
                update.setInt(1, bookId);
                update.executeUpdate();
                JOptionPane.showMessageDialog(null, "Book Issued!");
            } else {
                JOptionPane.showMessageDialog(null, "Book Unavailable or Invalid ID");
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(null, "Error: " + e.getMessage()); }
    }

    public static void loadIssuedBooks(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT t.id, b.title, m.name, t.issue_date, t.status " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.id " +
                     "JOIN members m ON t.member_id = m.id " +
                     "WHERE t.status = 'ISSUED'";
        
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("title"), rs.getString("name"), rs.getDate("issue_date"), rs.getString("status")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void returnBook(int transactionId) {
        try (Connection conn = connect()) {
            if (conn == null) return;
            PreparedStatement check = conn.prepareStatement("SELECT book_id, status FROM transactions WHERE id=?");
            check.setInt(1, transactionId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                if ("RETURNED".equals(rs.getString("status"))) {
                    JOptionPane.showMessageDialog(null, "This book is already returned.");
                    return;
                }
                
                int bookId = rs.getInt("book_id");

                PreparedStatement updateTrans = conn.prepareStatement("UPDATE transactions SET return_date=CURDATE(), status='RETURNED' WHERE id=?");
                updateTrans.setInt(1, transactionId);
                updateTrans.executeUpdate();
                PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET quantity = quantity + 1 WHERE id=?");
                updateBook.setInt(1, bookId);
                updateBook.executeUpdate();

                JOptionPane.showMessageDialog(null, "Book Returned Successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Transaction ID not found.");
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(null, "Error: " + e.getMessage()); }
    }

    public LibrarySystem() {
        setTitle("Library Management System");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Books", createBookPanel());
        tabs.addTab("Manage Members", createMemberPanel());
        tabs.addTab("Issue Books", createIssuePanel());
        tabs.addTab("Return Books", createReturnPanel()); // <--- NEW TAB

        add(tabs);
        setVisible(true);
    }

    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        JTextField tTitle = new JTextField(), tAuthor = new JTextField(), tGenre = new JTextField(), tQty = new JTextField();
        JButton btnAdd = new JButton("Add Book");
        
        inputPanel.add(new JLabel("Title:")); inputPanel.add(tTitle);
        inputPanel.add(new JLabel("Author:")); inputPanel.add(tAuthor);
        inputPanel.add(new JLabel("Genre:")); inputPanel.add(tGenre);
        inputPanel.add(new JLabel("Quantity:")); inputPanel.add(tQty);
        inputPanel.add(new JLabel("")); inputPanel.add(btnAdd);

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Genre", "Qty"}, 0);
        loadBooks(model);
        
        btnAdd.addActionListener(e -> {
            try {
                addBook(tTitle.getText(), tAuthor.getText(), tGenre.getText(), Integer.parseInt(tQty.getText()));
                loadBooks(model);
                tTitle.setText(""); tAuthor.setText(""); tGenre.setText(""); tQty.setText("");
            } catch (Exception ex) { JOptionPane.showMessageDialog(null, "Invalid Input"); }
        });

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMemberPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField tName = new JTextField(), tEmail = new JTextField(), tPhone = new JTextField();
        JButton btnAdd = new JButton("Add Member");

        inputPanel.add(new JLabel("Name:")); inputPanel.add(tName);
        inputPanel.add(new JLabel("Email:")); inputPanel.add(tEmail);
        inputPanel.add(new JLabel("Phone:")); inputPanel.add(tPhone);
        inputPanel.add(new JLabel("")); inputPanel.add(btnAdd);

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Phone"}, 0);
        loadMembers(model);

        btnAdd.addActionListener(e -> {
            addMember(tName.getText(), tEmail.getText(), tPhone.getText());
            loadMembers(model);
            tName.setText(""); tEmail.setText(""); tPhone.setText("");
        });

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createIssuePanel() {
        JPanel panel = new JPanel(new FlowLayout());
        JTextField tBookId = new JTextField(10);
        JTextField tMemId = new JTextField(10);
        JButton btnIssue = new JButton("Issue Book");

        panel.add(new JLabel("Book ID:")); panel.add(tBookId);
        panel.add(new JLabel("Member ID:")); panel.add(tMemId);
        panel.add(btnIssue);

        btnIssue.addActionListener(e -> {
            try {
                issueBook(Integer.parseInt(tBookId.getText()), Integer.parseInt(tMemId.getText()));
            } catch (Exception ex) { JOptionPane.showMessageDialog(null, "Invalid IDs"); }
        });
        return panel;
    }

    private JPanel createReturnPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Input Area
        JPanel inputPanel = new JPanel(new FlowLayout());
        JTextField tTransId = new JTextField(10);
        JButton btnReturn = new JButton("Return Book");
        JButton btnRefresh = new JButton("Refresh List");

        inputPanel.add(new JLabel("Transaction ID:")); inputPanel.add(tTransId);
        inputPanel.add(btnReturn);
        inputPanel.add(btnRefresh);

        String[] cols = {"Trans ID", "Book Title", "Member Name", "Issue Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        loadIssuedBooks(model);

        btnReturn.addActionListener(e -> {
            try {
                returnBook(Integer.parseInt(tTransId.getText()));
                loadIssuedBooks(model); // Refresh list after return
                tTransId.setText("");
            } catch (Exception ex) { JOptionPane.showMessageDialog(null, "Enter valid Transaction ID"); }
        });

        btnRefresh.addActionListener(e -> loadIssuedBooks(model));

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySystem());
    }
}