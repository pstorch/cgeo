package cgeo.geocaching.maps.download;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import java.util.ArrayList;


import io.reactivex.Single;
import okhttp3.Response;
import org.xml.sax.SAXException;

public class MapDownloadActivity extends AbstractActionBarActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(final Intent intent) {
        if (intent == null) {
            return;
        }

        final ArrayList<String> downloads = new ArrayList<>();
        if ("locus-actions".equals(intent.getScheme())) {
            final Uri data = intent.getData();
            if (data != null) {
                downloads.addAll(retrieveLocusActions(data.getHost() + "://" + data.getPath().substring(1)));
            }
        }

        if (downloads.isEmpty()) {
            finish();
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.setTitle(R.string.map_download_confirm)
                .setCancelable(true)
                .setMessage(getString(R.string.map_download_confirm_items, downloads))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.dismiss();
                        MapDownloadService.startService(MapDownloadActivity.this, downloads);
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .create();
        dialog.setOwnerActivity(this);
        dialog.show();
    }

    private ArrayList<String> retrieveLocusActions(final String uri) {
        final ArrayList<String> downloads = new ArrayList<>();
        final Single<Response> response = Network.getRequest(uri);
        final String document = Network.getResponseData(response);

        final RootElement root = new RootElement("locusActions");
        final Element download = root.getChild("download");
        download.getChild("source").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                try {
                    downloads.add(body);
                } catch (final Exception e) {
                    Log.w("Failed to parse download source", e);
                }
            }
        });

        try {
            Xml.parse(document, root.getContentHandler());
        } catch (final SAXException e) {
            throw new RuntimeException("Cannot parse locus-actions", e);
        }

        return downloads;
    }

}
