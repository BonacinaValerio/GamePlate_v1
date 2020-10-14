package com.bonacogo.gameplate;

import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * This activity has a margin.
 */
public class CaptureActivity extends com.journeyapps.barcodescanner.CaptureActivity {
    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_capture);
        return (DecoratedBarcodeView)findViewById(R.id.zxing_barcode_scanner);
    }
}
