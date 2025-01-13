

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class OrderManagementFrame extends JFrame {
    private JTable Ordertable;
    private DefaultTableModel ordermodel;
    private JButton Search, openAdminFrameB, Returned;
    private JTextField searchfield;
    private int clickCount = 0;
    private static final int DOUBLE_CLICK_THRESHOLD = 500;
    private static Timer resetTimer;

    public OrderManagementFrame() {
        setTitle("Order Management");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainpanel = new JPanel(new BorderLayout());
        JPanel searchpanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel searchlabel = new JLabel("Search by Book Name");
        gbc.gridx = 0;
        gbc.gridy = 0;
        searchpanel.add(searchlabel, gbc);

        searchfield = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        searchpanel.add(searchfield, gbc);

        Search = new JButton("Search");
        gbc.gridx = 2;
        gbc.gridy = 0;
        searchpanel.add(Search, gbc);

        openAdminFrameB = new JButton("Go to Admin");
        gbc.gridx = 3;
        gbc.gridy = 0;
        searchpanel.add(openAdminFrameB, gbc);

        // Add Returned button
        Returned = new JButton("Returned");
        gbc.gridx = 4;
        gbc.gridy = 0;
        searchpanel.add(Returned, gbc);

        String[] columns = {"Username", "Student Name", "Student ID", "Book Name", "Status", "Ordered Date", "Returned Date"};
        ordermodel = new DefaultTableModel(columns, 0);
        Ordertable = new JTable(ordermodel);
        JScrollPane orderScrollPane = new JScrollPane(Ordertable);

        mainpanel.add(searchpanel, BorderLayout.NORTH);
        mainpanel.add(orderScrollPane, BorderLayout.CENTER);

        add(mainpanel, BorderLayout.CENTER);

        loadAcceptedOrders();

        openAdminFrameB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AdminFrame().setVisible(true);
                setVisible(false);
            }
        });

        Search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickCount++;

                if (clickCount == 1) {
                    if (resetTimer != null) {
                        resetTimer.stop();
                    }
                    resetTimer = new Timer(DOUBLE_CLICK_THRESHOLD, evt -> {
                        clickCount = 0;
                    });
                    resetTimer.setRepeats(false);
                    resetTimer.start();
                }

                if (clickCount == 1) {
                    searchAcceptedOrders();
                } else if (clickCount == 2) {
                    ordermodel.setRowCount(0);
                    loadAcceptedOrders();
                    clickCount = 0;
                    if (resetTimer != null) {
                        resetTimer.stop();
                    }
                }
            }
        });

        Returned.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markAsReturned();
            }
        });
    }

    private void loadAcceptedOrders() {
        try {
            Connection con = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM OrderBook WHERE Status = 'Accept'";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            ordermodel.setRowCount(0);

            while (rs.next()) {
                String username = rs.getString("username");
                String studentName = rs.getString("Studentname");
                String studentId = rs.getString("StudentID");
                String bookName = rs.getString("Bookname");
                String status = rs.getString("Status");
                Date orderedDate = rs.getDate("OrderedDate");
                Date returnedDate = rs.getDate("ReturnedDate");

                ordermodel.addRow(new Object[]{username, studentName, studentId, bookName, status, orderedDate, returnedDate});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while loading the orders.");
        }
    }

    private void searchAcceptedOrders() {
        String bookName = searchfield.getText().trim();

        try {
            Connection con = DatabaseConnection.getConnection();
            String sql;
            PreparedStatement ps;

            if (bookName.isEmpty()) {
                sql = "SELECT * FROM OrderBook WHERE Status = 'Accept'";
                ps = con.prepareStatement(sql);
            } else {
                sql = "SELECT * FROM OrderBook WHERE Bookname LIKE ? AND Status = 'Accept'";
                ps = con.prepareStatement(sql);
                ps.setString(1, "%" + bookName + "%");
            }

            ResultSet rs = ps.executeQuery();

            ordermodel.setRowCount(0);

            while (rs.next()) {
                String username = rs.getString("username");
                String studentName = rs.getString("Studentname");
                String studentId = rs.getString("StudentID");
                String orderBookName = rs.getString("Bookname");
                String status = rs.getString("Status");
                Date orderedDate = rs.getDate("OrderedDate");
                Date returnedDate = rs.getDate("ReturnedDate");

                ordermodel.addRow(new Object[]{username, studentName, studentId, orderBookName, status, orderedDate, returnedDate});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while searching for orders.");
        }
    }

    private void markAsReturned() {
        int selectedRow = Ordertable.getSelectedRow();
        if (selectedRow != -1) {
            String studentId = ordermodel.getValueAt(selectedRow, 2).toString();
            String bookName = ordermodel.getValueAt(selectedRow, 3).toString();

            try {
                Connection con = DatabaseConnection.getConnection();

                String sql = "UPDATE OrderBook SET Status = 'Returned' WHERE StudentID = ? AND Bookname = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, studentId);
                ps.setString(2, bookName);

                int rowsUpdated = ps.executeUpdate();
                if (rowsUpdated > 0) {
                    JOptionPane.showMessageDialog(this, "Book returned successfully.");

                    ordermodel.removeRow(selectedRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to mark book as returned.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred while updating the order status.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an order to mark as returned.");
        }
    }

    public static void main(String[] args) {
        new OrderManagementFrame().setVisible(true);
    }
}
