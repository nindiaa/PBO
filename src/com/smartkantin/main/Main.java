package com.smartkantin.main;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartkantin.view.LoginForm; // Pastikan import LoginForm
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Hapus tanda // di depan kode di bawah ini:
        java.awt.EventQueue.invokeLater(() -> {
            new LoginForm().setVisible(true); 
        });
    }
}