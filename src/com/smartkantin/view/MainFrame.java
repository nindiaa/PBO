/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.smartkantin.view;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.smartkantin.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import javax.swing.JOptionPane;
import java.awt.event.KeyEvent; 
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
/**
 *
 * @author ACER
 */
public class MainFrame extends javax.swing.JFrame implements Runnable {
    private boolean isTransaksiActive = true; // Default saat aplikasi buka
    public static Webcam webcam = null;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName()); 
    private String menuAktif = "TRANSAKSI"; // Default
    
public MainFrame() {
        //initWebcam();
    initComponents();
    javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
        setPlaceholder(txtBarcode, "Barcode");
    setPlaceholder(txtQTY, "QTY");
    setPlaceholder(txtBayar, "0");
    setPlaceholder(txtDiskon, "0");
    setPlaceholder(txtBarcodeBarang, "Barcode");
    setPlaceholder(txtNamaBarang, "Nama Barang");
    setPlaceholder(txtHargaBarang, "Harga");
    setPlaceholder(txtStokBarang, "Stok");
  panelUtama.removeAll();
    panelUtama.add(panelTranksaksi);
    panelUtama.repaint();
    panelUtama.revalidate();
// Load data saat pertama kali buka
    loadDataBarang();
   
    // Ini kuncinya: Memaksa JFrame mengikuti ukuran komponen yang ada di dalamnya
    this.pack(); 
      tabelTransaksi.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {},
        new String [] {"Barcode", "Nama Barang", "Harga", "Qty", "SubTotal"}
    ));
    // Menaruh di tengah layar
    this.setLocationRelativeTo(null); 
    
    // Mencegah user mengubah ukuran jendela
    this.setResizable(false);
     txtBayar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) { update(); }
        @Override
        public void removeUpdate(DocumentEvent e) { update(); }
        @Override
        public void changedUpdate(DocumentEvent e) { update(); }
        
        private void update() {
            // Kita gunakan formatInputBayar secara hati-hati agar tidak looping
            // Tapi untuk denda/kembalian cukup panggil hitungKembalian()
            hitungKembalian();
        }
    });
}
private void loadDataBarang() {
    try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        ResultSet res = conn.createStatement().executeQuery("SELECT * FROM products");
        
        // Menggunakan model tabel standar Java
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tblDataBarang.getModel();
        model.setRowCount(0); // Bersihkan tabel
        
        while (res.next()) {
            model.addRow(new Object[] {
                res.getString("barcode"),
                res.getString("name"),
                res.getInt("price"),
                res.getInt("stock")
            });
        }
    } catch (Exception e) { 
        JOptionPane.showMessageDialog(this, "Gagal Load: " + e.getMessage()); 
    }
}

private void loadDataRiwayat() {
    // 1. Pastikan kita menggunakan variabel tabel yang benar (tblRiwayat)
    javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tblRiwayat.getModel();
    model.setRowCount(0); 
    
    try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        // 2. Query yang pasti jalan
        String sql = "SELECT id, created_at, total_amount FROM transactions ORDER BY created_at DESC";
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        
        while (rs.next()) {
            // 3. Masukkan ke tblRiwayat
            model.addRow(new Object[]{
                rs.getString("id"),
                "TRX-" + rs.getString("id"),
                rs.getString("created_at"),
                "Rp " + String.format("%,d", rs.getLong("total_amount")).replace(",", "."),
                "Admin"
            });
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal Load Riwayat: " + e.getMessage());
    }
}

private void loadDataUsers() {
    javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tblUsers.getModel();
    model.setRowCount(0);
    try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        ResultSet rs = conn.createStatement().executeQuery("SELECT username, role FROM users");
        while (rs.next()) {
            model.addRow(new Object[]{rs.getString("username"), rs.getString("role")});
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal Load Users: " + e.getMessage());
    }
}

private void tampilkanGrafik() {
    org.jfree.data.category.DefaultCategoryDataset dataset = new org.jfree.data.category.DefaultCategoryDataset();
    try {
        java.sql.Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        conn.createStatement().execute("SET sql_mode = ''");
        
        String sql = "SELECT DATE_FORMAT(created_at, '%d-%m') as tanggal, SUM(total_amount) as pendapatan " +
                     "FROM transactions WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                     "GROUP BY DATE(created_at) ORDER BY created_at ASC";
        
        java.sql.ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            dataset.addValue(rs.getDouble("pendapatan"), "Pendapatan", rs.getString("tanggal"));
        }
        
        org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
            "Grafik Pendapatan 7 Hari Terakhir", 
            "Tanggal", 
            "Rp", 
            dataset, 
            org.jfree.chart.plot.PlotOrientation.VERTICAL, 
            false, 
            true, 
            false
        );
        
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setRangeGridlinePaint(java.awt.Color.LIGHT_GRAY);
        
        org.jfree.chart.renderer.category.BarRenderer renderer = new org.jfree.chart.renderer.category.BarRenderer();
        
        // ====================================================================
        // INI KUNCI MUTLAK AGAR WARNA 100% SOLID DAN TIDAK ADA EFEK KACA PUTIH
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        // ====================================================================
        
        // 1. Set Warna Coklat Solid
        renderer.setSeriesPaint(0, new java.awt.Color(140, 90, 60)); 
        
        // 2. Matikan Efek Bayangan Hitam di belakang
        renderer.setShadowVisible(false); 
        
        // 3. Atur Lebar Maksimal Batang agar tidak lebar memenuhi layar
        renderer.setMaximumBarWidth(0.10); 
        
        plot.setRenderer(renderer);
        
        org.jfree.chart.ChartPanel chartPanel = new org.jfree.chart.ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1043, 524));
        
        panelGrafik.removeAll();
        panelGrafik.setLayout(new java.awt.BorderLayout());
        panelGrafik.add(chartPanel, java.awt.BorderLayout.CENTER);
        panelGrafik.validate();
        panelGrafik.repaint();
        
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this, "Error Grafik: " + e.getMessage());
        e.printStackTrace();
    }
}
// 6. Helper: Bersihkan Form
private void clearInputBarang() {
    txtBarcodeBarang.setText("Barcode"); // Balikin ke placeholder
    txtNamaBarang.setText("Nama Barang");
    txtHargaBarang.setText("Harga");
    txtStokBarang.setText("Stok");
    txtBarcodeBarang.setEditable(true);
    txtBarcodeBarang.requestFocus();
}
private void setPlaceholder(javax.swing.JTextField textField, String placeholder) {
    textField.setText(placeholder);
    textField.setForeground(java.awt.Color.GRAY);

    textField.addFocusListener(new java.awt.event.FocusListener() {
        @Override
        public void focusGained(java.awt.event.FocusEvent e) {
            if (textField.getText().equals(placeholder)) {
                textField.setText("");
                textField.setForeground(java.awt.Color.BLACK);
            }
        }
        @Override
        public void focusLost(java.awt.event.FocusEvent e) {
            if (textField.getText().isEmpty()) {
                textField.setText(placeholder);
                textField.setForeground(java.awt.Color.GRAY);
            }
        }
    });
}
    private void formatInputBayar() {
          String input = txtBayar.getText().replace(".", "");
    if (input.matches("\\d+")) {
        long angka = Long.parseLong(input);
        txtBayar.setText(String.format("%,d", angka).replace(",", "."));
    }
    }
    
     private void hitungKembalian() {
         try {
        // Ambil Total Tagihan
        String totalStr = lblTotal.getText().replace("Rp ", "").replace(".", "");
        if (totalStr.isEmpty()) totalStr = "0";
        double total = Double.parseDouble(totalStr);

        // Ambil Bayar (Hilangkan titik)
        String bayarStr = txtBayar.getText().replace(".", "");
        if (bayarStr.isEmpty()) bayarStr = "0";
        double bayar = Double.parseDouble(bayarStr);

        // Hitung Kembalian
        double kembali = bayar - total;
        
        // Tampilkan ke lblKembalian dengan format Rupiah
        String hasil = String.format("Rp %,.0f", kembali).replace(",", ".");
        lblKembalian.setText(hasil);

        // Warna Merah jika kurang bayar, Hijau jika cukup
        if (kembali < 0) {
            lblKembalian.setForeground(Color.RED);
        } else {
            lblKembalian.setForeground(new Color(140, 90, 60)); // Warna coklat tema kamu
        }
    } catch (Exception e) {
        lblKembalian.setText("Rp 0");
    }
    }
     
private void hitungTotalTagihan() {
    javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
    long totalSebelumDiskon = 0;
    
    // Hitung total dari tabel
    for (int i = 0; i < model.getRowCount(); i++) {
        totalSebelumDiskon += Long.parseLong(model.getValueAt(i, 4).toString());
    }
    
    // Ambil nilai diskon dari txtDiskon
    String strDiskon = txtDiskon.getText().replace(".", "");
    long diskon = strDiskon.isEmpty() ? 0 : Long.parseLong(strDiskon);
    
    // Hitung Total Akhir
    long totalAkhir = totalSebelumDiskon - diskon;
    
    // Tampilkan
    lblTotal.setText("Rp " + String.format("%,d", totalAkhir).replace(",", "."));
    
    // Panggil hitungKembalian agar saat total berubah, kembalian otomatis update
    hitungKembalian();
}  
     

private void initWebcam(javax.swing.JPanel targetPanel) {
    if (webcam != null && webcam.isOpen()) {
        webcam.close();
    }

    webcam = Webcam.getDefault();
    if (webcam != null) {
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setPreferredSize(new java.awt.Dimension(328, 177));
        
        targetPanel.setLayout(new java.awt.BorderLayout());
        targetPanel.removeAll();
        targetPanel.add(panel, java.awt.BorderLayout.CENTER);
        targetPanel.revalidate();
        targetPanel.repaint();
        
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    } else {
        JOptionPane.showMessageDialog(this, "Tidak ada kamera ditemukan!");
    }
}

private void stopWebcam() {
    if (webcam != null && webcam.isOpen()) {
        webcam.close();
    }
    // Bersihkan KEDUA panel
    panelKamera.removeAll();
    panelKamera.repaint();
    panelKamera.revalidate();
    
    pnlKamera.removeAll();
    pnlKamera.repaint();
    pnlKamera.revalidate();
}
    
@Override
public void run() {
    while (true) {
        if (webcam != null && webcam.isOpen()) {
            try {
                com.google.zxing.Result result = null;
                java.awt.image.BufferedImage image = webcam.getImage();
                
                if (image != null) {
                    com.google.zxing.LuminanceSource source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
                    com.google.zxing.BinaryBitmap bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
                    result = new com.google.zxing.MultiFormatReader().decode(bitmap);
                }

                if (result != null) {
                    final String kode = result.getText();
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        // Cek penanda menu
                        if (menuAktif.equals("TRANSAKSI")) {
                            txtBarcode.setText(kode);
                            processBarcode(kode);
                        } else if (menuAktif.equals("BARANG")) {
                            txtBarcodeBarang.setText(kode);
                        }
                    });
                    Thread.sleep(2000); // Jeda biar tidak scan terus menerus
                }
            } catch (Exception e) {}
        }
        try { Thread.sleep(100); } catch (Exception e) {}
    }
}



private void processBarcode(String kode) {
 try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "SELECT * FROM products WHERE barcode='" + kode + "'";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        
        if (rs.next()) {
            String nama = rs.getString("name");
            long harga = rs.getLong("price");
            
            // PERBAIKAN: Jika input kosong atau placeholder, set jadi 1
            String qtyText = txtQTY.getText().trim();
            int qty = (qtyText.isEmpty() || qtyText.equals("QTY")) ? 1 : Integer.parseInt(qtyText);
            
            long subTotal = harga * qty;
            
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
            model.addRow(new Object[]{kode, nama, harga, qty, subTotal});
            
            txtBarcode.setText("");
            txtQTY.setText("QTY"); // Balikin ke placeholder
            txtQTY.setForeground(java.awt.Color.GRAY);
            txtBarcode.requestFocus();
            hitungTotalTagihan();
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error Scan: " + e.getMessage());
    }
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Sidebar = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        btnMenuTransaksi = new javax.swing.JButton();
        btnMenuDataBarang = new javax.swing.JButton();
        btnMenuLaporan = new javax.swing.JButton();
        btnMenuPengguna = new javax.swing.JButton();
        btnMenuRiwayat = new javax.swing.JButton();
        btnMenuLogout = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        panelUtama = new javax.swing.JPanel();
        panelTranksaksi = new javax.swing.JPanel();
        jButton8 = new javax.swing.JButton();
        lblKembalian = new javax.swing.JTextField();
        txtBayar = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblTotal = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        panelKamera = new javax.swing.JPanel();
        btnCetak = new javax.swing.JButton();
        btnTambah = new javax.swing.JButton();
        txtQTY = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtBarcode = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabelTransaksi = new javax.swing.JTable();
        jLabel9 = new javax.swing.JLabel();
        txtDiskon = new javax.swing.JTextField();
        btnRefresh = new javax.swing.JButton();
        btnAktifkanKamera1 = new javax.swing.JButton();
        panelDataBarang = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblDataBarang = new javax.swing.JTable();
        txtBarcodeBarang = new javax.swing.JTextField();
        pnlKamera = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        txtNamaBarang = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        txtHargaBarang = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        txtStokBarang = new javax.swing.JTextField();
        btnTambahBarang = new javax.swing.JButton();
        btnHapusBarang = new javax.swing.JButton();
        btnEditBarang = new javax.swing.JButton();
        btnRefreshBarang = new javax.swing.JButton();
        btnAktifkanKamera = new javax.swing.JButton();
        panelLaporan = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        panelGrafik = new javax.swing.JPanel();
        panelRiwayat = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblRiwayat = new javax.swing.JTable();
        btnExportPDF = new javax.swing.JToggleButton();
        jLabel14 = new javax.swing.JLabel();
        panelPengguna = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblUsers = new javax.swing.JTable();
        txtUsername = new javax.swing.JTextField();
        cbKelas = new javax.swing.JComboBox<>();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        jLabel17 = new javax.swing.JLabel();
        btnSimpanUser = new javax.swing.JButton();
        btnEditUser = new javax.swing.JButton();
        btnRefreshUser = new javax.swing.JButton();
        btnHapusUser = new javax.swing.JButton();
        jLabel18 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Sidebar.setBackground(new java.awt.Color(140, 90, 60));
        Sidebar.setForeground(new java.awt.Color(140, 90, 60));

        jLabel1.setBackground(new java.awt.Color(255, 248, 240));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 248, 240));
        jLabel1.setText("SmartKantin");

        btnMenuTransaksi.setBackground(new java.awt.Color(140, 90, 60));
        btnMenuTransaksi.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMenuTransaksi.setForeground(new java.awt.Color(255, 248, 240));
        btnMenuTransaksi.setText("Tranksaksi");
        btnMenuTransaksi.setBorder(null);
        btnMenuTransaksi.addActionListener(this::btnMenuTransaksiActionPerformed);

        btnMenuDataBarang.setBackground(new java.awt.Color(140, 90, 60));
        btnMenuDataBarang.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMenuDataBarang.setForeground(new java.awt.Color(255, 248, 240));
        btnMenuDataBarang.setText("Data Barang");
        btnMenuDataBarang.setBorder(null);
        btnMenuDataBarang.addActionListener(this::btnMenuDataBarangActionPerformed);

        btnMenuLaporan.setBackground(new java.awt.Color(140, 90, 60));
        btnMenuLaporan.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMenuLaporan.setForeground(new java.awt.Color(255, 248, 240));
        btnMenuLaporan.setText("Laporan Keuangan");
        btnMenuLaporan.setBorder(null);
        btnMenuLaporan.addActionListener(this::btnMenuLaporanActionPerformed);

        btnMenuPengguna.setBackground(new java.awt.Color(140, 90, 60));
        btnMenuPengguna.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMenuPengguna.setForeground(new java.awt.Color(255, 248, 240));
        btnMenuPengguna.setText("Kelola Pengguna");
        btnMenuPengguna.setBorder(null);
        btnMenuPengguna.addActionListener(this::btnMenuPenggunaActionPerformed);

        btnMenuRiwayat.setBackground(new java.awt.Color(140, 90, 60));
        btnMenuRiwayat.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMenuRiwayat.setForeground(new java.awt.Color(255, 248, 240));
        btnMenuRiwayat.setText("Riwayat Tranksaksi");
        btnMenuRiwayat.setBorder(null);
        btnMenuRiwayat.addActionListener(this::btnMenuRiwayatActionPerformed);

        btnMenuLogout.setBackground(new java.awt.Color(140, 90, 60));
        btnMenuLogout.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnMenuLogout.setForeground(new java.awt.Color(255, 248, 240));
        btnMenuLogout.setText("LogOut");
        btnMenuLogout.setBorder(null);
        btnMenuLogout.addActionListener(this::btnMenuLogoutActionPerformed);

        javax.swing.GroupLayout SidebarLayout = new javax.swing.GroupLayout(Sidebar);
        Sidebar.setLayout(SidebarLayout);
        SidebarLayout.setHorizontalGroup(
            SidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SidebarLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(SidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMenuTransaksi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMenuDataBarang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMenuLaporan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMenuRiwayat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMenuPengguna, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnMenuLogout, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(32, Short.MAX_VALUE))
        );
        SidebarLayout.setVerticalGroup(
            SidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SidebarLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel1)
                .addGap(66, 66, 66)
                .addComponent(btnMenuTransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnMenuDataBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnMenuLaporan, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnMenuRiwayat, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnMenuPengguna, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 163, Short.MAX_VALUE)
                .addComponent(btnMenuLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(74, 74, 74))
        );

        getContentPane().add(Sidebar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setPreferredSize(new java.awt.Dimension(1050, 720));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        panelUtama.setBackground(new java.awt.Color(255, 248, 240));
        panelUtama.setLayout(new java.awt.CardLayout());

        panelTranksaksi.setBackground(new java.awt.Color(255, 248, 240));
        panelTranksaksi.setForeground(new java.awt.Color(255, 248, 240));

        jButton8.setBackground(new java.awt.Color(140, 90, 60));
        jButton8.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jButton8.setForeground(new java.awt.Color(255, 248, 240));
        jButton8.setText("Bayar");
        jButton8.addActionListener(this::jButton8ActionPerformed);

        lblKembalian.setBackground(new java.awt.Color(255, 248, 240));

        txtBayar.setBackground(new java.awt.Color(255, 248, 240));
        txtBayar.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                txtBayarComponentShown(evt);
            }
        });
        txtBayar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBayarKeyReleased(evt);
            }
        });

        jLabel8.setBackground(new java.awt.Color(140, 90, 60));
        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(140, 90, 60));
        jLabel8.setText("Kembalian");

        jLabel7.setBackground(new java.awt.Color(140, 90, 60));
        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(140, 90, 60));
        jLabel7.setText("Bayar Rp");

        lblTotal.setBackground(new java.awt.Color(140, 90, 60));
        lblTotal.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        lblTotal.setForeground(new java.awt.Color(140, 90, 60));
        lblTotal.setText("Rp 0");

        jLabel5.setBackground(new java.awt.Color(140, 90, 60));
        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(140, 90, 60));
        jLabel5.setText("Total Tagihan :");

        panelKamera.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnCetak.setBackground(new java.awt.Color(75, 46, 43));
        btnCetak.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnCetak.setForeground(new java.awt.Color(255, 248, 240));
        btnCetak.setText("Cetak Struk");
        btnCetak.addActionListener(this::btnCetakActionPerformed);

        btnTambah.setBackground(new java.awt.Color(140, 90, 60));
        btnTambah.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnTambah.setForeground(new java.awt.Color(255, 248, 240));
        btnTambah.setText("Tambah");
        btnTambah.addActionListener(this::btnTambahActionPerformed);

        txtQTY.setBackground(new java.awt.Color(255, 248, 240));

        jLabel4.setBackground(new java.awt.Color(140, 90, 60));
        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(140, 90, 60));
        jLabel4.setText("QTY");

        txtBarcode.setBackground(new java.awt.Color(255, 248, 240));
        txtBarcode.addActionListener(this::txtBarcodeActionPerformed);
        txtBarcode.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBarcodeKeyReleased(evt);
            }
        });

        jLabel3.setBackground(new java.awt.Color(140, 90, 60));
        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(140, 90, 60));
        jLabel3.setText("Barcode");

        tabelTransaksi.setBackground(new java.awt.Color(255, 248, 240));
        tabelTransaksi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Barcode", "Nama Barang", "Harga", "Qty", "SubTotal"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tabelTransaksi);

        jLabel9.setBackground(new java.awt.Color(140, 90, 60));
        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(140, 90, 60));
        jLabel9.setText("Diskon");

        txtDiskon.setBackground(new java.awt.Color(255, 248, 240));
        txtDiskon.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtDiskonKeyReleased(evt);
            }
        });

        btnRefresh.setBackground(new java.awt.Color(75, 46, 43));
        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRefresh.setForeground(new java.awt.Color(255, 248, 240));
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(this::btnRefreshActionPerformed);

        btnAktifkanKamera1.setBackground(new java.awt.Color(43, 28, 26));
        btnAktifkanKamera1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnAktifkanKamera1.setForeground(new java.awt.Color(255, 248, 240));
        btnAktifkanKamera1.setText("Camera");
        btnAktifkanKamera1.addActionListener(this::btnAktifkanKamera1ActionPerformed);

        javax.swing.GroupLayout panelTranksaksiLayout = new javax.swing.GroupLayout(panelTranksaksi);
        panelTranksaksi.setLayout(panelTranksaksiLayout);
        panelTranksaksiLayout.setHorizontalGroup(
            panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTranksaksiLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelTranksaksiLayout.createSequentialGroup()
                        .addComponent(btnAktifkanKamera1, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelTranksaksiLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBarcode, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtQTY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnTambah, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCetak, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelTranksaksiLayout.createSequentialGroup()
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(panelTranksaksiLayout.createSequentialGroup()
                            .addGap(53, 53, 53)
                            .addComponent(panelKamera, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTranksaksiLayout.createSequentialGroup()
                        .addGap(53, 53, 53)
                        .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(txtDiskon, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
                                .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING))
                            .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(lblKembalian, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
                                .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblTotal, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtBayar, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING)))))
                .addGap(37, 37, 37))
        );
        panelTranksaksiLayout.setVerticalGroup(
            panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTranksaksiLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelTranksaksiLayout.createSequentialGroup()
                        .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCetak, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnTambah, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)
                            .addComponent(txtQTY, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(txtBarcode, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 537, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelTranksaksiLayout.createSequentialGroup()
                        .addComponent(panelKamera, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBayar, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblKembalian, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDiskon, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelTranksaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAktifkanKamera1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        panelUtama.add(panelTranksaksi, "card2");

        panelDataBarang.setBackground(new java.awt.Color(255, 248, 240));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(140, 90, 60));
        jLabel6.setText("Data Barang");

        tblDataBarang.setBackground(new java.awt.Color(255, 248, 240));
        tblDataBarang.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Barcode", "Nama Barang", "Harga", "Stok"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblDataBarang.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblDataBarangMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(tblDataBarang);

        txtBarcodeBarang.setBackground(new java.awt.Color(255, 248, 240));
        txtBarcodeBarang.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBarcodeBarangKeyReleased(evt);
            }
        });

        pnlKamera.setPreferredSize(new java.awt.Dimension(327, 177));

        javax.swing.GroupLayout pnlKameraLayout = new javax.swing.GroupLayout(pnlKamera);
        pnlKamera.setLayout(pnlKameraLayout);
        pnlKameraLayout.setHorizontalGroup(
            pnlKameraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlKameraLayout.setVerticalGroup(
            pnlKameraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 177, Short.MAX_VALUE)
        );

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(140, 90, 60));
        jLabel10.setText("Kode Barcode");

        txtNamaBarang.setBackground(new java.awt.Color(255, 248, 240));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(140, 90, 60));
        jLabel11.setText("Nama Barang");

        txtHargaBarang.setBackground(new java.awt.Color(255, 248, 240));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(140, 90, 60));
        jLabel12.setText("Harga");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(140, 90, 60));
        jLabel13.setText("Stok");

        txtStokBarang.setBackground(new java.awt.Color(255, 248, 240));

        btnTambahBarang.setBackground(new java.awt.Color(140, 90, 60));
        btnTambahBarang.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnTambahBarang.setForeground(new java.awt.Color(255, 248, 240));
        btnTambahBarang.setText("Tambah Barang");
        btnTambahBarang.addActionListener(this::btnTambahBarangActionPerformed);

        btnHapusBarang.setBackground(new java.awt.Color(204, 0, 0));
        btnHapusBarang.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnHapusBarang.setForeground(new java.awt.Color(255, 248, 240));
        btnHapusBarang.setText("Hapus");
        btnHapusBarang.addActionListener(this::btnHapusBarangActionPerformed);

        btnEditBarang.setBackground(new java.awt.Color(75, 46, 43));
        btnEditBarang.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnEditBarang.setForeground(new java.awt.Color(255, 248, 240));
        btnEditBarang.setText("Edit");
        btnEditBarang.addActionListener(this::btnEditBarangActionPerformed);

        btnRefreshBarang.setBackground(new java.awt.Color(43, 28, 26));
        btnRefreshBarang.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRefreshBarang.setForeground(new java.awt.Color(255, 248, 240));
        btnRefreshBarang.setText("Refresh");
        btnRefreshBarang.addActionListener(this::btnRefreshBarangActionPerformed);

        btnAktifkanKamera.setBackground(new java.awt.Color(43, 28, 26));
        btnAktifkanKamera.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnAktifkanKamera.setForeground(new java.awt.Color(255, 248, 240));
        btnAktifkanKamera.setText("Camera");
        btnAktifkanKamera.addActionListener(this::btnAktifkanKameraActionPerformed);

        javax.swing.GroupLayout panelDataBarangLayout = new javax.swing.GroupLayout(panelDataBarang);
        panelDataBarang.setLayout(panelDataBarangLayout);
        panelDataBarangLayout.setHorizontalGroup(
            panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDataBarangLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addGroup(panelDataBarangLayout.createSequentialGroup()
                        .addGroup(panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(panelDataBarangLayout.createSequentialGroup()
                                .addComponent(btnAktifkanKamera, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnRefreshBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnEditBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnHapusBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 689, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(40, 40, 40)
                        .addGroup(panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pnlKamera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtBarcodeBarang)
                            .addComponent(jLabel10)
                            .addComponent(txtNamaBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                            .addComponent(jLabel11)
                            .addComponent(txtHargaBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                            .addComponent(jLabel12)
                            .addComponent(txtStokBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                            .addComponent(jLabel13)
                            .addComponent(btnTambahBarang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        panelDataBarangLayout.setVerticalGroup(
            panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDataBarangLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addGroup(panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 538, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelDataBarangLayout.createSequentialGroup()
                        .addComponent(pnlKamera, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel10)
                        .addGap(1, 1, 1)
                        .addComponent(txtBarcodeBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel11)
                        .addGap(1, 1, 1)
                        .addComponent(txtNamaBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel12)
                        .addGap(1, 1, 1)
                        .addComponent(txtHargaBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel13)
                        .addGap(1, 1, 1)
                        .addComponent(txtStokBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnTambahBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDataBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnHapusBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEditBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRefreshBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAktifkanKamera, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(35, Short.MAX_VALUE))
        );

        panelUtama.add(panelDataBarang, "card3");

        panelLaporan.setBackground(new java.awt.Color(255, 248, 240));

        jLabel2.setBackground(new java.awt.Color(140, 90, 60));
        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(140, 90, 60));
        jLabel2.setText("Laporan Keungan");

        panelGrafik.setBackground(new java.awt.Color(255, 248, 240));

        javax.swing.GroupLayout panelGrafikLayout = new javax.swing.GroupLayout(panelGrafik);
        panelGrafik.setLayout(panelGrafikLayout);
        panelGrafikLayout.setHorizontalGroup(
            panelGrafikLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1043, Short.MAX_VALUE)
        );
        panelGrafikLayout.setVerticalGroup(
            panelGrafikLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 524, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout panelLaporanLayout = new javax.swing.GroupLayout(panelLaporan);
        panelLaporan.setLayout(panelLaporanLayout);
        panelLaporanLayout.setHorizontalGroup(
            panelLaporanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLaporanLayout.createSequentialGroup()
                .addGroup(panelLaporanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLaporanLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2))
                    .addGroup(panelLaporanLayout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addComponent(panelGrafik, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        panelLaporanLayout.setVerticalGroup(
            panelLaporanLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLaporanLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(panelGrafik, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(74, Short.MAX_VALUE))
        );

        panelUtama.add(panelLaporan, "card4");

        panelRiwayat.setBackground(new java.awt.Color(255, 248, 240));

        tblRiwayat.setBackground(new java.awt.Color(255, 248, 240));
        tblRiwayat.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID", "Kode TRX", "Tanggal", "Total", "Kasir"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane3.setViewportView(tblRiwayat);

        btnExportPDF.setBackground(new java.awt.Color(43, 28, 26));
        btnExportPDF.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnExportPDF.setForeground(new java.awt.Color(255, 248, 240));
        btnExportPDF.setText("Export PDF");
        btnExportPDF.addActionListener(this::btnExportPDFActionPerformed);

        jLabel14.setBackground(new java.awt.Color(140, 90, 60));
        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(140, 90, 60));
        jLabel14.setText("Riwayat Tranksaksi");

        javax.swing.GroupLayout panelRiwayatLayout = new javax.swing.GroupLayout(panelRiwayat);
        panelRiwayat.setLayout(panelRiwayatLayout);
        panelRiwayatLayout.setHorizontalGroup(
            panelRiwayatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRiwayatLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(panelRiwayatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addGroup(panelRiwayatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(btnExportPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 1030, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        panelRiwayatLayout.setVerticalGroup(
            panelRiwayatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRiwayatLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel14)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 509, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnExportPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        panelUtama.add(panelRiwayat, "card5");

        panelPengguna.setBackground(new java.awt.Color(255, 248, 240));

        tblUsers.setBackground(new java.awt.Color(255, 248, 240));
        tblUsers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Username", "Role"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblUsers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblUsersMouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(tblUsers);

        txtUsername.setBackground(new java.awt.Color(255, 248, 240));
        txtUsername.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtUsername.setForeground(new java.awt.Color(140, 90, 60));

        cbKelas.setBackground(new java.awt.Color(255, 248, 240));
        cbKelas.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        cbKelas.setForeground(new java.awt.Color(140, 90, 60));
        cbKelas.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Admin", "User", " " }));
        cbKelas.setPreferredSize(new java.awt.Dimension(321, 38));

        jLabel15.setBackground(new java.awt.Color(140, 90, 60));
        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(140, 90, 60));
        jLabel15.setText("Username");

        jLabel16.setBackground(new java.awt.Color(140, 90, 60));
        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(140, 90, 60));
        jLabel16.setText("Password");

        txtPassword.setBackground(new java.awt.Color(255, 248, 240));
        txtPassword.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtPassword.setForeground(new java.awt.Color(140, 90, 60));
        txtPassword.setPreferredSize(new java.awt.Dimension(321, 38));

        jLabel17.setBackground(new java.awt.Color(140, 90, 60));
        jLabel17.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(140, 90, 60));
        jLabel17.setText("Role");

        btnSimpanUser.setBackground(new java.awt.Color(140, 90, 60));
        btnSimpanUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSimpanUser.setForeground(new java.awt.Color(255, 248, 240));
        btnSimpanUser.setText("Simpan");
        btnSimpanUser.addActionListener(this::btnSimpanUserActionPerformed);

        btnEditUser.setBackground(new java.awt.Color(75, 46, 43));
        btnEditUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnEditUser.setForeground(new java.awt.Color(255, 248, 240));
        btnEditUser.setText("Edit");
        btnEditUser.addActionListener(this::btnEditUserActionPerformed);

        btnRefreshUser.setBackground(new java.awt.Color(75, 46, 43));
        btnRefreshUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRefreshUser.setForeground(new java.awt.Color(255, 248, 240));
        btnRefreshUser.setText("Refresh");

        btnHapusUser.setBackground(new java.awt.Color(204, 0, 0));
        btnHapusUser.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnHapusUser.setForeground(new java.awt.Color(255, 248, 240));
        btnHapusUser.setText("Hapus");
        btnHapusUser.addActionListener(this::btnHapusUserActionPerformed);

        jLabel18.setBackground(new java.awt.Color(140, 90, 60));
        jLabel18.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(140, 90, 60));
        jLabel18.setText("Kelola Akun");

        javax.swing.GroupLayout panelPenggunaLayout = new javax.swing.GroupLayout(panelPengguna);
        panelPengguna.setLayout(panelPenggunaLayout);
        panelPenggunaLayout.setHorizontalGroup(
            panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPenggunaLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPenggunaLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(panelPenggunaLayout.createSequentialGroup()
                        .addGroup(panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(panelPenggunaLayout.createSequentialGroup()
                                .addComponent(btnRefreshUser, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(323, 323, 323)
                                .addComponent(btnHapusUser, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 651, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 70, Short.MAX_VALUE)
                        .addGroup(panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtUsername)
                            .addComponent(cbKelas, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel15)
                            .addComponent(jLabel16)
                            .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel17)
                            .addComponent(btnSimpanUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnEditUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(24, 24, 24))))
        );
        panelPenggunaLayout.setVerticalGroup(
            panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPenggunaLayout.createSequentialGroup()
                .addGroup(panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelPenggunaLayout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 557, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelPenggunaLayout.createSequentialGroup()
                        .addGap(129, 129, 129)
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(23, 23, 23)
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbKelas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSimpanUser, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnEditUser, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPenggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnRefreshUser, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnHapusUser, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        panelUtama.add(panelPengguna, "card6");

        jPanel2.add(panelUtama, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 50, 1080, 670));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 0, 1090, 720));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtBayarComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_txtBayarComponentShown
        // TODO add your handling code here:
    }//GEN-LAST:event_txtBayarComponentShown

    private void btnMenuTransaksiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuTransaksiActionPerformed
    stopWebcam();
    panelUtama.removeAll();
    panelUtama.add(panelTranksaksi);
    panelUtama.repaint();
    panelUtama.revalidate();
    menuAktif = "TRANSAKSI"; // Tandai menu aktif
    }//GEN-LAST:event_btnMenuTransaksiActionPerformed

    private void btnMenuDataBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuDataBarangActionPerformed
      stopWebcam();
    panelUtama.removeAll();
    panelUtama.add(panelDataBarang);
    panelUtama.repaint();
    panelUtama.revalidate();
    menuAktif = "BARANG"; // Tandai menu aktif
    }//GEN-LAST:event_btnMenuDataBarangActionPerformed

    private void txtBarcodeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBarcodeKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
        // Begitu di-scan (Enter), pindahkan fokus ke kolom QTY
        txtQTY.requestFocus();
        txtQTY.selectAll(); // Memudahkan kasir langsung menimpa angka 1
    }
    }//GEN-LAST:event_txtBarcodeKeyReleased

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
      // 1. Validasi Keranjang Kosong
    if (tabelTransaksi.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "Keranjang kosong!");
        return;
    }

    try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        conn.setAutoCommit(false); // Transaksi dimulai

        // 2. Simpan ke tabel 'transactions'
        // Kolom DB: total_amount, cash_paid, change_amount
        String sqlTrans = "INSERT INTO transactions (total_amount, cash_paid, change_amount) VALUES (?, ?, ?)";
        PreparedStatement pstTrans = conn.prepareStatement(sqlTrans, Statement.RETURN_GENERATED_KEYS);
        
        long total = Long.parseLong(lblTotal.getText().replace("Rp ", "").replace(".", "").replace(",", ""));
        long bayar = Long.parseLong(txtBayar.getText().replace(".", "").replace(",", ""));
        long kembali = bayar - total;
        
        pstTrans.setLong(1, total);
        pstTrans.setLong(2, bayar);
        pstTrans.setLong(3, kembali);
        pstTrans.executeUpdate();

        // Ambil ID transaksi terbaru
        ResultSet rs = pstTrans.getGeneratedKeys();
        int idTransaksi = 0;
        if (rs.next()) idTransaksi = rs.getInt(1);

        // 3. Loop Tabel untuk Insert Detail dan Update Stok
        String sqlDetail = "INSERT INTO transaction_details (transaction_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        PreparedStatement pstDetail = conn.prepareStatement(sqlDetail);
        
        String sqlUpdateStok = "UPDATE products SET stock = stock - ? WHERE barcode = ?";
        PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdateStok);

        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            String barcode = model.getValueAt(i, 0).toString();
            int qty = Integer.parseInt(model.getValueAt(i, 3).toString());
            long subTotal = Long.parseLong(model.getValueAt(i, 4).toString());

            // Insert Detail
            pstDetail.setInt(1, idTransaksi);
            pstDetail.setString(2, barcode); // product_id di db sesuai barcode
            pstDetail.setInt(3, qty);
            pstDetail.setLong(4, subTotal);
            pstDetail.executeUpdate();

            // Kurangi Stok
            pstUpdate.setInt(1, qty);
            pstUpdate.setString(2, barcode);
            pstUpdate.executeUpdate();
        }

        conn.commit(); 
        JOptionPane.showMessageDialog(this, "Transaksi Berhasil!");

        // 4. Reset Form & Refresh Data
        model.setRowCount(0);
        lblTotal.setText("Rp 0");
        txtBayar.setText("");
        lblKembalian.setText("");
        txtBarcode.requestFocus();
        
        // Refresh tabel Data Barang agar stok berkurang di tampilan
        loadDataBarang();

    } catch (Exception e) {
        try {
            com.smartkantin.config.DatabaseConnection.getConnection().rollback();
        } catch (Exception ex) {}
        JOptionPane.showMessageDialog(this, "Gagal Bayar: " + e.getMessage());
        e.printStackTrace();
    }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void txtBayarKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBayarKeyReleased
            formatInputBayar();
        // TODO add your handling code here:
    }//GEN-LAST:event_txtBayarKeyReleased

    private void btnTambahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahActionPerformed
         String barcode = txtBarcode.getText().trim();
    if (barcode.equals("Barcode") || barcode.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Scan atau masukkan barcode terlebih dahulu!");
        return;
    }

    try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "SELECT * FROM products WHERE barcode='" + barcode + "'";
        ResultSet rs = conn.createStatement().executeQuery(sql);

        if (rs.next()) {
            String nama = rs.getString("name");
            long harga = rs.getLong("price");
            int stockDb = rs.getInt("stock");
            int qty = txtQTY.getText().isEmpty() || txtQTY.getText().equals("QTY") ? 1 : Integer.parseInt(txtQTY.getText());

            // Validasi Stok
            if (qty > stockDb) {
                JOptionPane.showMessageDialog(this, "Stok tidak mencukupi! Sisa stok: " + stockDb);
                return;
            }

            long subTotal = harga * qty;

            // Tambah ke tabel transaksi
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
            model.addRow(new Object[]{barcode, nama, harga, qty, subTotal});

            // Reset input
            txtBarcode.setText("");
            txtQTY.setText("");
            txtBarcode.requestFocus();

            hitungTotalTagihan();
        } else {
            JOptionPane.showMessageDialog(this, "Barang tidak ditemukan di database!");
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }

    }//GEN-LAST:event_btnTambahActionPerformed

    private void checkStokMinimum(String barcode) {
    try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "SELECT name, stock FROM products WHERE barcode = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, barcode);
        ResultSet rs = pst.executeQuery();
        
        if (rs.next()) {
            int stok = rs.getInt("stock");
            String nama = rs.getString("name");
            
            // Notifikasi jika stok di bawah 5
            if (stok <= 5) {
                JOptionPane.showMessageDialog(this, "PERINGATAN! Stok barang " + nama + " tinggal " + stok + "!", 
                        "Stok Hampir Habis", JOptionPane.WARNING_MESSAGE);
            }
        }
    } catch (Exception e) {
        System.err.println("Error Cek Stok: " + e.getMessage());
    }
}
    
    private void txtBarcodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBarcodeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtBarcodeActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
         javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
    model.setRowCount(0);
    
    // 2. Reset semua input dan label
    txtBarcode.setText("");
    txtQTY.setText("");
    txtBayar.setText("0");
    txtDiskon.setText("0");
    lblTotal.setText("Rp 0");
    lblKembalian.setText("Rp 0");
    
    // 3. Fokus kembali ke barcode
    txtBarcode.requestFocus();
    
    JOptionPane.showMessageDialog(this, "Keranjang telah dibersihkan!");        // TODO add your handling code here:
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void txtDiskonKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDiskonKeyReleased
          String input = txtDiskon.getText().replace(".", "");
    if (input.matches("\\d+")) {
        long angka = Long.parseLong(input);
        txtDiskon.setText(String.format("%,d", angka).replace(",", "."));
    }
    
    // Hitung ulang tagihan setiap kali ada perubahan angka diskon
    hitungTotalTagihan();          // TODO add your handling code here:
    }//GEN-LAST:event_txtDiskonKeyReleased

    private void txtBarcodeBarangKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBarcodeBarangKeyReleased
         if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
        try {
            Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM products WHERE barcode='" + txtBarcodeBarang.getText() + "'");
            if (rs.next()) {
                txtNamaBarang.setText(rs.getString("name"));
                txtHargaBarang.setText(rs.getString("price"));
                txtStokBarang.setText(rs.getString("stock"));
            }
        } catch (Exception e) {}
    }        // TODO add your handling code here:
    }//GEN-LAST:event_txtBarcodeBarangKeyReleased

    private void tblDataBarangMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblDataBarangMouseClicked
           int baris = tblDataBarang.getSelectedRow();
    txtBarcodeBarang.setText(tblDataBarang.getValueAt(baris, 0).toString());
    txtNamaBarang.setText(tblDataBarang.getValueAt(baris, 1).toString());
    txtHargaBarang.setText(tblDataBarang.getValueAt(baris, 2).toString());
    txtStokBarang.setText(tblDataBarang.getValueAt(baris, 3).toString());
    txtBarcodeBarang.setEditable(false);
    }//GEN-LAST:event_tblDataBarangMouseClicked

    private void btnTambahBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahBarangActionPerformed
                try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "INSERT INTO products (barcode, name, price, stock) VALUES (?,?,?,?)";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, txtBarcodeBarang.getText());
        pst.setString(2, txtNamaBarang.getText());
        pst.setString(3, txtHargaBarang.getText());
        pst.setString(4, txtStokBarang.getText());
        pst.executeUpdate();
        JOptionPane.showMessageDialog(this, "Berhasil Menambah Barang!");
        loadDataBarang();
        clearInputBarang();
    } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }  // TODO add your handling code here:
    }//GEN-LAST:event_btnTambahBarangActionPerformed

    private void btnHapusBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusBarangActionPerformed
      try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        conn.createStatement().execute("DELETE FROM products WHERE barcode='" + txtBarcodeBarang.getText() + "'");
        JOptionPane.showMessageDialog(this, "Barang Dihapus!");
        loadDataBarang();
        clearInputBarang();
    } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }//GEN-LAST:event_btnHapusBarangActionPerformed

    private void btnEditBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditBarangActionPerformed
        try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "UPDATE products SET name=?, price=?, stock=? WHERE barcode=?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, txtNamaBarang.getText());
        pst.setString(2, txtHargaBarang.getText());
        pst.setString(3, txtStokBarang.getText());
        pst.setString(4, txtBarcodeBarang.getText());
        pst.executeUpdate();
        JOptionPane.showMessageDialog(this, "Berhasil Update Barang!");
        loadDataBarang();
        clearInputBarang();
    } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
    }//GEN-LAST:event_btnEditBarangActionPerformed

    private void btnAktifkanKameraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAktifkanKameraActionPerformed
   if (webcam == null || !webcam.isOpen()) {
        initWebcam(pnlKamera); // <-- Kirim pnlKamera (Data Barang)
        btnAktifkanKamera.setText("Matikan Kamera");
    } else {
        stopWebcam();
        btnAktifkanKamera.setText("Camera");
    }
    }//GEN-LAST:event_btnAktifkanKameraActionPerformed

    private void btnAktifkanKamera1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAktifkanKamera1ActionPerformed
   if (webcam == null || !webcam.isOpen()) {
        initWebcam(panelKamera); // <-- Kirim panelKamera (Transaksi)
        btnAktifkanKamera1.setText("Matikan Kamera");
    } else {
        stopWebcam();
        btnAktifkanKamera1.setText("Camera");
    }
    }//GEN-LAST:event_btnAktifkanKamera1ActionPerformed

    private void btnRefreshBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshBarangActionPerformed
           txtBarcodeBarang.setText("Barcode");
    txtBarcodeBarang.setForeground(java.awt.Color.GRAY);
    txtNamaBarang.setText("Nama Barang");
    txtNamaBarang.setForeground(java.awt.Color.GRAY);
    txtHargaBarang.setText("Harga");
    txtHargaBarang.setForeground(java.awt.Color.GRAY);
    txtStokBarang.setText("Stok");
    txtStokBarang.setForeground(java.awt.Color.GRAY);
    
    txtBarcodeBarang.setEditable(true);
    txtBarcodeBarang.requestFocus();
    
    JOptionPane.showMessageDialog(this, "Form telah dibersihkan!");        // TODO add your handling code here:
    }//GEN-LAST:event_btnRefreshBarangActionPerformed

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakActionPerformed
           if (tabelTransaksi.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "Keranjang kosong!");
        return;
    }
    
    try {
        String namaFile = "Struk_" + System.currentTimeMillis() + ".txt";
        java.io.FileWriter writer = new java.io.FileWriter(namaFile);
        
        writer.write("       SMART KANTIN       \n");
        writer.write("==========================\n");
        
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tabelTransaksi.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            writer.write(model.getValueAt(i, 1) + "  " + model.getValueAt(i, 3) + "x  " + model.getValueAt(i, 4) + "\n");
        }
        
        writer.write("==========================\n");
        writer.write("Total Tagihan: " + lblTotal.getText() + "\n");
        writer.write("       Terima Kasih       \n");
        writer.close();
        
        // Membuka file struk secara otomatis
        java.awt.Desktop.getDesktop().open(new java.io.File(namaFile));
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal Cetak: " + e.getMessage());
    }        // TODO add your handling code here:
    }//GEN-LAST:event_btnCetakActionPerformed

    private void btnMenuLaporanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuLaporanActionPerformed
         stopWebcam();
    panelUtama.removeAll();
    panelUtama.add(panelLaporan); // Ini panel yang ada 'panelGrafik'-nya
    panelUtama.repaint();
    panelUtama.revalidate();
    menuAktif = "LAPORAN"; 
    
    tampilkanGrafik(); // Ini memanggil fungsi untuk menggambar chart        // TODO add your handling code here:
    }//GEN-LAST:event_btnMenuLaporanActionPerformed

    private void btnMenuRiwayatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuRiwayatActionPerformed
        stopWebcam();
    panelUtama.removeAll();
    panelUtama.add(panelRiwayat);
    panelUtama.repaint();
    panelUtama.revalidate();
    
    // Panggil fungsi load data
    loadDataRiwayat();        // TODO add your handling code here:
    }//GEN-LAST:event_btnMenuRiwayatActionPerformed

    private void btnExportPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportPDFActionPerformed
  if (tblRiwayat.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "Tidak ada data untuk diexport!");
        return;
    }

    try {
        String namaFile = "Riwayat_Transaksi_" + System.currentTimeMillis() + ".pdf";
        com.lowagie.text.Document doc = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(namaFile));
        
        doc.open();
        doc.add(new com.lowagie.text.Paragraph("LAPORAN RIWAYAT TRANSAKSI"));
        doc.add(new com.lowagie.text.Paragraph(" "));
        
        com.lowagie.text.pdf.PdfPTable pdfTable = new com.lowagie.text.pdf.PdfPTable(5);
        pdfTable.addCell("ID");
        pdfTable.addCell("Kode TRX");
        pdfTable.addCell("Tanggal");
        pdfTable.addCell("Total");
        pdfTable.addCell("Kasir");
        
        // Loop dengan pengecekan null
        for (int i = 0; i < tblRiwayat.getRowCount(); i++) {
            for (int j = 0; j < 5; j++) {
                Object value = tblRiwayat.getValueAt(i, j);
                // Jika nilai null, ganti dengan string kosong
                pdfTable.addCell(value != null ? value.toString() : "-");
            }
        }
        
        doc.add(pdfTable);
        doc.close();
        
        java.awt.Desktop.getDesktop().open(new java.io.File(namaFile));
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal Export: " + e.getMessage());
        e.printStackTrace();
    }
    }//GEN-LAST:event_btnExportPDFActionPerformed

    private void btnSimpanUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanUserActionPerformed
        try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, txtUsername.getText());
        pst.setString(2, new String(txtPassword.getPassword())); // Menggunakan JPasswordField
        pst.setString(3, cbKelas.getSelectedItem().toString());
        pst.executeUpdate();
        
        JOptionPane.showMessageDialog(this, "User berhasil disimpan!");
        loadDataUsers();
        clearInputUser();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal Simpan: " + e.getMessage());
    }        // TODO add your handling code here:
    }//GEN-LAST:event_btnSimpanUserActionPerformed

    private void btnEditUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditUserActionPerformed
         try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "UPDATE users SET password=?, role=? WHERE username=?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, new String(txtPassword.getPassword()));
        pst.setString(2, cbKelas.getSelectedItem().toString());
        pst.setString(3, txtUsername.getText());
        pst.executeUpdate();
        loadDataUsers();
        clearInputUser();
    } catch (Exception e) { JOptionPane.showMessageDialog(this, "Gagal Edit: " + e.getMessage()); }        // TODO add your handling code here:
    }//GEN-LAST:event_btnEditUserActionPerformed

    private void btnHapusUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusUserActionPerformed
        try {
        Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
        String sql = "DELETE FROM users WHERE username = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, txtUsername.getText());
        pst.executeUpdate();
        loadDataUsers();
        clearInputUser();
    } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }        // TODO add your handling code here:
    }//GEN-LAST:event_btnHapusUserActionPerformed

    private void tblUsersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblUsersMouseClicked
          int baris = tblUsers.getSelectedRow();
    txtUsername.setText(tblUsers.getValueAt(baris, 0).toString());
    cbKelas.setSelectedItem(tblUsers.getValueAt(baris, 1).toString());
    txtUsername.setEditable(false); // Jangan ubah username saat edit        // TODO add your handling code here:
    }//GEN-LAST:event_tblUsersMouseClicked

    private void btnMenuPenggunaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuPenggunaActionPerformed
           stopWebcam();
    panelUtama.removeAll();
    panelUtama.add(panelPengguna);
    panelUtama.repaint();
    panelUtama.revalidate();
    
    // Panggil fungsi load data
    loadDataUsers();        // TODO add your handling code here:                // TODO add your handling code here:
    }//GEN-LAST:event_btnMenuPenggunaActionPerformed

    private void btnMenuLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuLogoutActionPerformed
         int konfirmasi = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin keluar dari sistem?", "Konfirmasi Logout", JOptionPane.YES_NO_OPTION);
    
    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            // 2. WAJIB: Matikan Kamera agar lampu webcam mati dan tidak error
            stopWebcam();
            
            // 3. Catat ke Log Aktivitas (Biar dosen makin terkesan)
            Connection conn = com.smartkantin.config.DatabaseConnection.getConnection();
            String sqlLog = "INSERT INTO activity_logs (activity, created_at) VALUES (?, NOW())";
            PreparedStatement pstLog = conn.prepareStatement(sqlLog);
            pstLog.setString(1, "Admin Logout dari sistem"); 
            pstLog.executeUpdate();
            
            // 4. Tutup halaman MainFrame ini
            this.dispose();
            
            // 5. Buka kembali halaman LoginForm
            // (Pastikan nama file form login kamu benar-benar LoginForm)
            LoginForm login = new LoginForm();
            login.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saat Logout: " + e.getMessage());
        }
    }        // TODO add your handling code here:
    }//GEN-LAST:event_btnMenuLogoutActionPerformed

private void clearInputUser() {
    txtUsername.setText("");
    txtPassword.setText("");
    cbKelas.setSelectedIndex(0);
    txtUsername.setEditable(true);
    txtUsername.requestFocus();
}

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Sidebar;
    private javax.swing.JButton btnAktifkanKamera;
    private javax.swing.JButton btnAktifkanKamera1;
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnEditBarang;
    private javax.swing.JButton btnEditUser;
    private javax.swing.JToggleButton btnExportPDF;
    private javax.swing.JButton btnHapusBarang;
    private javax.swing.JButton btnHapusUser;
    private javax.swing.JButton btnMenuDataBarang;
    private javax.swing.JButton btnMenuLaporan;
    private javax.swing.JButton btnMenuLogout;
    private javax.swing.JButton btnMenuPengguna;
    private javax.swing.JButton btnMenuRiwayat;
    private javax.swing.JButton btnMenuTransaksi;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnRefreshBarang;
    private javax.swing.JButton btnRefreshUser;
    private javax.swing.JButton btnSimpanUser;
    private javax.swing.JButton btnTambah;
    private javax.swing.JButton btnTambahBarang;
    private javax.swing.JComboBox<String> cbKelas;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextField lblKembalian;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JPanel panelDataBarang;
    private javax.swing.JPanel panelGrafik;
    private javax.swing.JPanel panelKamera;
    private javax.swing.JPanel panelLaporan;
    private javax.swing.JPanel panelPengguna;
    private javax.swing.JPanel panelRiwayat;
    private javax.swing.JPanel panelTranksaksi;
    private javax.swing.JPanel panelUtama;
    private javax.swing.JPanel pnlKamera;
    private javax.swing.JTable tabelTransaksi;
    private javax.swing.JTable tblDataBarang;
    private javax.swing.JTable tblRiwayat;
    private javax.swing.JTable tblUsers;
    private javax.swing.JTextField txtBarcode;
    private javax.swing.JTextField txtBarcodeBarang;
    private javax.swing.JTextField txtBayar;
    private javax.swing.JTextField txtDiskon;
    private javax.swing.JTextField txtHargaBarang;
    private javax.swing.JTextField txtNamaBarang;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtQTY;
    private javax.swing.JTextField txtStokBarang;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
