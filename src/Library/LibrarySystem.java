package Library;

import java.awt.*; 
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class LibrarySystem extends JFrame {

    // ==========================================
    // 1. CONFIGURATION
    // ==========================================
    private static final String URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String USER = "root";
    private static final String PASS = "Sujith@123"; // <--- UPDATE PASSWORD

    // ==========================================
    // 2. MAIN UI SETUP
    // ==========================================
    private JTabbedPane tabbedPane;
    private JLabel lblTotalIssued, lblTotalReturned, lblPending;
    private JTable tblAllBooks, tblAllMembers;

    public LibrarySystem() {
        setTitle("Library System Pro (Strict Mode)");
        setSize(1150, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- TABS ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        tabbedPane.addTab("🏠 Dashboard", createDashboardPanel());
        tabbedPane.addTab("📖 Issue Book", createIssuePanel());
        tabbedPane.addTab("🔙 Return & Renew", createStandardReturnPanel());
        tabbedPane.addTab("⚠️ Lost / Damaged", createLostDamagedPanel());
        tabbedPane.addTab("📚 Manage Books", createBooksPanel());
        tabbedPane.addTab("👤 Members", createMembersPanel());

        add(tabbedPane);
        
        tabbedPane.addChangeListener(e -> refreshAllData());
        refreshAllData();
        setVisible(true);
    }

    // ==========================================
    // 3. DATABASE HELPER
    // ==========================================
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private void loadTableData(JTable table, String sql) {
        new Thread(() -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            Vector<Vector<Object>> data = new Vector<>();
            try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                int colCount = rs.getMetaData().getColumnCount();
                Vector<String> columns = new Vector<>();
                for (int i = 1; i <= colCount; i++) columns.add(rs.getMetaData().getColumnName(i));
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for (int i = 1; i <= colCount; i++) row.add(rs.getObject(i));
                    data.add(row);
                }
                SwingUtilities.invokeLater(() -> model.setDataVector(data, columns));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ==========================================
    // 4. TAB PANELS
    // ==========================================

    // --- DASHBOARD ---
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 1. Header
        JLabel header = new JLabel("<html><h1 style='padding:20px; color:#333;'>Library Analytics Dashboard</h1></html>", SwingConstants.CENTER);
        panel.add(header, BorderLayout.NORTH);

        // 2. Stats Panel (Center)
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 30, 30));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 50, 30));

        lblTotalIssued = createStatCard("Total Issued", "0", new Color(230, 240, 255));
        lblTotalReturned = createStatCard("Total Returned", "0", new Color(230, 255, 240));
        lblPending = createStatCard("Active Pending", "0", new Color(255, 230, 230));

        // Click Events for Cards
        lblTotalIssued.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { showDetailsDialog("History", "SELECT * FROM issue_records"); }});
        lblTotalReturned.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { showDetailsDialog("Returned", "SELECT * FROM issue_records WHERE return_date IS NOT NULL"); }});
        lblPending.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { showDetailsDialog("Pending", "SELECT * FROM issue_records WHERE return_date IS NULL"); }});

        statsPanel.add(lblTotalIssued); statsPanel.add(lblTotalReturned); statsPanel.add(lblPending);
        panel.add(statsPanel, BorderLayout.CENTER);

        // 3. Special Actions Panel (South)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionsPanel.setBorder(new EmptyBorder(20, 0, 50, 0));
        
        JButton btnLostLog = new JButton("⚠️ View Lost / Damaged History Table");
        btnLostLog.setBackground(new Color(255, 100, 100)); // Light Red
        btnLostLog.setForeground(Color.WHITE);
        btnLostLog.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLostLog.setOpaque(true); 
        btnLostLog.setBorderPainted(false);
        btnLostLog.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnLostLog.addActionListener(e -> showDetailsDialog("Lost & Damaged Log", 
            "SELECT issue_id, book_id, member_id, fine_amount, payment_status, status, lost_report_date FROM issue_records WHERE status IN ('LOST', 'DAMAGED', 'REPORTED_LOST')"));

        actionsPanel.add(btnLostLog);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private JLabel createStatCard(String title, String value, Color bg) {
        JLabel l = new JLabel("<html><center><h2>" + title + "</h2><h1>" + value + "</h1><br><font size='2' color='gray'>(Click Details)</font></center></html>", SwingConstants.CENTER);
        l.setOpaque(true); l.setBackground(bg); l.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }
    
    private void showDetailsDialog(String title, String sql) {
        JDialog d = new JDialog(this, title, true); d.setSize(900, 500); d.setLocationRelativeTo(this);
        JTable t = new JTable(new DefaultTableModel()); d.add(new JScrollPane(t)); loadTableData(t, sql); d.setVisible(true);
    }

    // --- ISSUE BOOK (UPDATED LOGIC HERE) ---
    private JPanel createIssuePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        
        JTextField bId = new JTextField(10);
        JTextField mId = new JTextField(10);
        JCheckBox chkOverride = new JCheckBox("Librarian Override");
        chkOverride.setForeground(Color.RED);

        JButton btn = new JButton("Confirm Issue");
        btn.setBackground(new Color(50, 150, 50)); btn.setForeground(Color.WHITE);
        btn.setOpaque(true); btn.setBorderPainted(false);

        form.add(new JLabel("Book ID:")); form.add(bId);
        form.add(new JLabel("Member ID:")); form.add(mId);
        form.add(chkOverride); form.add(btn);

        JTextArea info = new JTextArea("\n RULES:\n 1. Limit: Max 2 Books.\n 2. Auto-Block: Overdue > 30 days.\n 3. Strict Block: CANNOT borrow if Lost/Damaged dues exist.");
        info.setEditable(false); info.setBackground(new Color(245, 245, 245));
        panel.add(form, BorderLayout.NORTH); panel.add(info, BorderLayout.CENTER);

        btn.addActionListener(e -> { 
            performIssue(bId.getText(), mId.getText(), chkOverride.isSelected()); 
            bId.setText(""); mId.setText(""); chkOverride.setSelected(false);
        });
        return panel;
    }

    // --- STANDARD RETURN ---
    private JPanel createStandardReturnPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        JTextField bId = new JTextField(8), mId = new JTextField(8);
        JButton btnCheck = new JButton("🔍 Check Dues");

        inputPanel.add(new JLabel("Book ID:")); inputPanel.add(bId);
        inputPanel.add(new JLabel("Member ID:")); inputPanel.add(mId);
        inputPanel.add(btnCheck);

        JPanel details = new JPanel(new GridLayout(4, 1, 10, 10)); details.setBorder(BorderFactory.createTitledBorder("Status"));
        JLabel lDays = new JLabel("Days: -"), lFine = new JLabel("Fine: ₹0.0");
        lFine.setForeground(Color.RED); details.add(lDays); details.add(lFine);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        JCheckBox chkPaid = new JCheckBox("Fine Paid?");
        JButton btnRenew = new JButton("🔄 Renew");
        JButton btnReturn = new JButton("✅ Return");
        btnReturn.setBackground(new Color(50, 150, 50)); btnReturn.setForeground(Color.WHITE); btnReturn.setEnabled(false);
        btnReturn.setOpaque(true); btnReturn.setBorderPainted(false);

        actions.add(btnRenew); actions.add(chkPaid); actions.add(btnReturn);

        panel.add(inputPanel, BorderLayout.NORTH); panel.add(details, BorderLayout.CENTER); panel.add(actions, BorderLayout.SOUTH);

        btnCheck.addActionListener(e -> calculateFine(bId.getText(), mId.getText(), lDays, lFine, btnReturn));
        btnRenew.addActionListener(e -> performRenew(bId.getText(), mId.getText()));
        btnReturn.addActionListener(e -> {
             double fine = Double.parseDouble(lFine.getText().replaceAll("[^0-9.]", ""));
             performFinalReturn(bId.getText(), mId.getText(), fine, chkPaid.isSelected(), true, "RETURNED");
             bId.setText(""); mId.setText(""); lDays.setText("-"); lFine.setText("₹0.0"); btnReturn.setEnabled(false);
        });
        return panel;
    }

    // --- LOST / DAMAGED ---
    private JPanel createLostDamagedPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Group 1: Inputs
        JPanel inputGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JTextField bId = new JTextField(10);
        JTextField mId = new JTextField(10);
        inputGroup.add(new JLabel("Book ID:")); inputGroup.add(bId);
        inputGroup.add(new JLabel("Member ID:")); inputGroup.add(mId);
        
        gbc.gridy = 0; panel.add(inputGroup, gbc);

        // Group 2: Incident Type
        JPanel typeGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        typeGroup.setBorder(BorderFactory.createTitledBorder("1. Incident Type"));
        JRadioButton rLost = new JRadioButton("Book LOST");
        JRadioButton rDamaged = new JRadioButton("Book DAMAGED");
        ButtonGroup bg = new ButtonGroup(); bg.add(rLost); bg.add(rDamaged); rLost.setSelected(true);
        typeGroup.add(rLost); typeGroup.add(rDamaged);
        
        gbc.gridy = 1; panel.add(typeGroup, gbc);

        // Group 3: Financial
        JPanel financeGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        financeGroup.setBorder(BorderFactory.createTitledBorder("2. Financial Action"));
        JLabel lblCost = new JLabel("₹500"); lblCost.setForeground(new Color(220, 50, 50)); lblCost.setFont(new Font("Arial", Font.BOLD, 22)); 
        JCheckBox chkPaid = new JCheckBox("Mark as Paid");
        financeGroup.add(new JLabel("Fine Amount:")); financeGroup.add(lblCost); financeGroup.add(new JSeparator(SwingConstants.VERTICAL)); financeGroup.add(chkPaid);
        
        gbc.gridy = 2; panel.add(financeGroup, gbc);

        // Buttons
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton btnReport = new JButton("Report Only");
        btnReport.setBackground(new Color(100, 100, 255)); btnReport.setForeground(Color.WHITE); btnReport.setOpaque(true); btnReport.setBorderPainted(false);
        JButton btnPay = new JButton("Pay & Close Case");
        btnPay.setBackground(new Color(255, 140, 0)); btnPay.setForeground(Color.WHITE); btnPay.setOpaque(true); btnPay.setBorderPainted(false);
        btnGroup.add(btnReport); btnGroup.add(btnPay);
        
        gbc.gridy = 3; gbc.insets = new Insets(20, 10, 10, 10); panel.add(btnGroup, gbc);

        // Spacer
        JPanel spacer = new JPanel(); gbc.gridy = 4; gbc.weighty = 1.0; panel.add(spacer, gbc);

        // Logic
        rLost.addActionListener(e -> lblCost.setText("₹500"));
        rDamaged.addActionListener(e -> lblCost.setText("₹300"));
        btnReport.addActionListener(e -> { if(!bId.getText().isEmpty()) performReportLost(bId.getText(), mId.getText()); });
        btnPay.addActionListener(e -> { if(!bId.getText().isEmpty()) performFinalReturn(bId.getText(), mId.getText(), rDamaged.isSelected()?300:500, true, rDamaged.isSelected(), rDamaged.isSelected()?"DAMAGED":"LOST"); });

        return panel;
    }

    // --- MANAGE ---
    private JPanel createBooksPanel() {
        JPanel p = new JPanel(new BorderLayout()); JPanel f = new JPanel();
        JTextField t = new JTextField(10), a = new JTextField(10), q = new JTextField(5); JButton b = new JButton("Add"); 
        f.add(new JLabel("Title")); f.add(t); f.add(new JLabel("Auth")); f.add(a); f.add(new JLabel("Qty")); f.add(q); f.add(b);
        tblAllBooks = new JTable(new DefaultTableModel(new String[]{"ID", "Title", "Author", "Qty"}, 0));
        p.add(f, BorderLayout.NORTH); p.add(new JScrollPane(tblAllBooks), BorderLayout.CENTER);
        b.addActionListener(e -> addBook(t.getText(), a.getText(), q.getText())); return p;
    }

    private JPanel createMembersPanel() {
        JPanel p = new JPanel(new BorderLayout()); JPanel f = new JPanel();
        JTextField n = new JTextField(10), em = new JTextField(10); JButton b = new JButton("Add"); 
        f.add(new JLabel("Name")); f.add(n); f.add(new JLabel("Email")); f.add(em); f.add(b);
        tblAllMembers = new JTable(new DefaultTableModel(new String[]{"ID", "Name", "Email"}, 0));
        p.add(f, BorderLayout.NORTH); p.add(new JScrollPane(tblAllMembers), BorderLayout.CENTER);
        b.addActionListener(e -> addMember(n.getText(), em.getText())); return p;
    }

    // ==========================================
    // 5. CORE LOGIC
    // ==========================================
    
    private void refreshAllData() {
        new Thread(() -> {
            try (Connection c = connect(); Statement s = c.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT (SELECT COUNT(*) FROM issue_records) as t, (SELECT COUNT(*) FROM issue_records WHERE return_date IS NOT NULL) as r, (SELECT COUNT(*) FROM issue_records WHERE return_date IS NULL) as p");
                if (rs.next()) {
                    int t=rs.getInt("t"), r=rs.getInt("r"), p=rs.getInt("p");
                    SwingUtilities.invokeLater(() -> {
                        lblTotalIssued.setText("<html><center><h2>Total Issued</h2><h1>"+t+"</h1><br><font size='2' color='gray'>(Click Details)</font></center></html>");
                        lblTotalReturned.setText("<html><center><h2>Returned</h2><h1>"+r+"</h1><br><font size='2' color='gray'>(Click Details)</font></center></html>");
                        lblPending.setText("<html><center><h2>Pending</h2><h1>"+p+"</h1><br><font size='2' color='gray'>(Click Details)</font></center></html>");
                    });
                }
            } catch (Exception e) {}
            loadTableData(tblAllBooks, "SELECT * FROM books");
            loadTableData(tblAllMembers, "SELECT * FROM members");
        }).start();
    }

    // --- UPDATED ISSUE LOGIC ---
    private void performIssue(String bId, String mId, boolean override) {
        if(bId.isEmpty() || mId.isEmpty()) return;
        new Thread(() -> {
            try (Connection c = connect()) {
                c.setAutoCommit(false);
                if (!override) {
                    // Check 1: Overdue Books > 37 days (Grace period passed)
                    PreparedStatement chk1 = c.prepareStatement("SELECT COUNT(*) FROM issue_records WHERE member_id=? AND return_date IS NULL AND DATEDIFF(CURDATE(), issue_date) > 37");
                    chk1.setInt(1, Integer.parseInt(mId)); ResultSet rs1 = chk1.executeQuery();
                    if(rs1.next() && rs1.getInt(1) > 0) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ BLOCKED: Overdue Book > 1 Month.")); return; }

                    // Check 2: STRICT LOST/DAMAGED BLOCK (Modified)
                    // Checks if user has ANY book marked as REPORTED_LOST, or LOST/DAMAGED but not yet marked 'PAID'
                    String sqlLostCheck = "SELECT COUNT(*) FROM issue_records WHERE member_id=? AND (status='REPORTED_LOST' OR (status IN ('LOST', 'DAMAGED') AND payment_status!='PAID'))";
                    PreparedStatement chk2 = c.prepareStatement(sqlLostCheck);
                    chk2.setInt(1, Integer.parseInt(mId)); ResultSet rs2 = chk2.executeQuery();
                    if(rs2.next() && rs2.getInt(1) > 0) { 
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ BLOCKED: You have an Unpaid Lost/Damaged Book.\nClear dues to borrow new books.")); 
                        return; 
                    }

                    // Check 3: Max Limit
                    PreparedStatement chk3 = c.prepareStatement("SELECT COUNT(*) FROM issue_records WHERE member_id=? AND return_date IS NULL");
                    chk3.setInt(1, Integer.parseInt(mId)); ResultSet rs3 = chk3.executeQuery();
                    if(rs3.next() && rs3.getInt(1) >= 2) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ LIMIT: Max 2 Books.")); return; }
                }
                
                // Perform Issue if all checks passed
                PreparedStatement stock = c.prepareStatement("SELECT quantity FROM books WHERE book_id=?"); stock.setInt(1, Integer.parseInt(bId)); ResultSet rs = stock.executeQuery();
                if(rs.next() && rs.getInt(1)>0) {
                    c.prepareStatement("INSERT INTO issue_records (book_id, member_id, issue_date, renewal_count, status) VALUES ("+bId+", "+mId+", CURDATE(), 0, 'PENDING')").executeUpdate();
                    c.prepareStatement("UPDATE books SET quantity=quantity-1 WHERE book_id="+bId).executeUpdate();
                    c.commit(); SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "✅ Issued!"); refreshAllData(); });
                } else SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ Out of Stock"));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void performReportLost(String bId, String mId) {
        new Thread(() -> {
            try (Connection c = connect()) {
                // Sets status to REPORTED_LOST. This triggers the block in performIssue immediately.
                PreparedStatement ps = c.prepareStatement("UPDATE issue_records SET lost_report_date=CURDATE(), status='REPORTED_LOST' WHERE book_id=? AND member_id=? AND return_date IS NULL");
                ps.setInt(1, Integer.parseInt(bId)); ps.setInt(2, Integer.parseInt(mId));
                if(ps.executeUpdate()>0) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "📅 Incident Reported. Borrowing BLOCKED until paid."));
                else SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ Not Found"));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void performFinalReturn(String bId, String mId, double amt, boolean paid, boolean restock, String status) {
        new Thread(() -> {
            try (Connection c = connect()) {
                c.setAutoCommit(false);
                String pay = paid ? "PAID" : "PENDING";
                PreparedStatement up = c.prepareStatement("UPDATE issue_records SET return_date=CURDATE(), fine_amount=?, payment_status=?, status=? WHERE book_id=? AND member_id=? AND return_date IS NULL");
                up.setDouble(1, amt); up.setString(2, pay); up.setString(3, status); up.setInt(4, Integer.parseInt(bId)); up.setInt(5, Integer.parseInt(mId));
                if(up.executeUpdate() > 0) {
                    if(restock) c.prepareStatement("UPDATE books SET quantity=quantity+1 WHERE book_id="+bId).executeUpdate();
                    c.commit(); SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "✅ Closed: " + status); refreshAllData(); });
                } else SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ Not Found"));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void calculateFine(String bId, String mId, JLabel lD, JLabel lF, JButton btn) {
        new Thread(() -> { try(Connection c=connect()){ PreparedStatement p=c.prepareStatement("SELECT DATEDIFF(CURDATE(),issue_date) as d FROM issue_records WHERE book_id=? AND member_id=? AND return_date IS NULL"); p.setInt(1,Integer.parseInt(bId)); p.setInt(2,Integer.parseInt(mId)); ResultSet rs=p.executeQuery(); if(rs.next()){ int d=rs.getInt("d"); double f=(d>7)?(d-7)*10:0; SwingUtilities.invokeLater(()->{ lD.setText("Days: "+d); lF.setText("Fine: "+f); btn.setEnabled(true); }); } }catch(Exception e){} }).start();
    }
    
    private void performRenew(String bId, String mId) { new Thread(()->{ try(Connection c=connect()){ PreparedStatement p=c.prepareStatement("UPDATE issue_records SET issue_date=CURDATE(), renewal_count=renewal_count+1 WHERE book_id=? AND member_id=? AND return_date IS NULL AND renewal_count=0"); p.setInt(1,Integer.parseInt(bId)); p.setInt(2,Integer.parseInt(mId)); if(p.executeUpdate()>0) SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,"✅ Renewed")); else SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,"❌ Limit Reached")); }catch(Exception e){} }).start(); }
    private void addBook(String t, String a, String q) { if(t.isEmpty()) return; new Thread(() -> { try(Connection c=connect()){ c.prepareStatement("INSERT INTO books(title, author, quantity) VALUES('"+t+"','"+a+"',"+q+")").executeUpdate(); refreshAllData(); }catch(Exception e){} }).start(); }
    private void addMember(String n, String em) { if(n.isEmpty()) return; new Thread(() -> { try(Connection c=connect()){ c.prepareStatement("INSERT INTO members(name, email) VALUES('"+n+"','"+em+"')").executeUpdate(); refreshAllData(); }catch(Exception e){} }).start(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibrarySystem::new);
    }
}