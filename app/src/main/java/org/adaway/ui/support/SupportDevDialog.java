package org.adaway.ui.support;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.net.Uri;

import org.adaway.R;

import timber.log.Timber;

/**
 * Helper to show a "Support Dev" chooser dialog with crypto QR codes and PayPal.
 */
public class SupportDevDialog {

    private static final String TRC20_ADDRESS = "TNusFveeu7mEyRLZK5Yw89byaRLWr9LXFM";
    private static final String BSC_ADDRESS = "0x6c774DD8c4e8712E3Ac1FDc93be8F53BD47FbeD1";

    private SupportDevDialog() {
    }

    /**
     * Show a chooser dialog with donation options.
     *
     * @param context The context to use for dialogs.
     */
    public static void show(Context context) {
        String[] options = {"TRC20 (USDT)", "BSC (BNB/USDT)", "PayPal"};
        new MaterialAlertDialogBuilder(context)
                .setTitle("Support Dev")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showQrDialog(context, "TRC20 (USDT)", TRC20_ADDRESS);
                            break;
                        case 1:
                            showQrDialog(context, "BSC (BNB/USDT)", BSC_ADDRESS);
                            break;
                        case 2:
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/moretoget")));
                            break;
                    }
                })
                .show();
    }

    private static void showQrDialog(Context context, String title, String address) {
        View view = LayoutInflater.from(context).inflate(R.layout.support_dev_qr_dialog, null);
        ImageView qrImageView = view.findViewById(R.id.qrImageView);
        TextView addressTextView = view.findViewById(R.id.addressTextView);

        Bitmap qrBitmap = generateQrCode(address, 512);
        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        }
        addressTextView.setText(address);

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Copy", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Address", address));
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.button_close, null)
                .show();
    }

    private static Bitmap generateQrCode(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Timber.e(e, "Failed to generate QR code");
            return null;
        }
    }
}
