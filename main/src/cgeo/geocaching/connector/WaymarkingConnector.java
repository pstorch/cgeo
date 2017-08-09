package cgeo.geocaching.connector;

import cgeo.geocaching.models.Geocache;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import org.apache.commons.lang3.StringUtils;

class WaymarkingConnector extends AbstractConnector {

    @Override
    @NonNull
    public String getName() {
        return "Waymarking";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return "WM";
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.waymarking.com";
    }

    @Override
    public boolean getHttps() {
        return false;
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        // this connector has no user management
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return "http://" + getHost() + "/waymarks/";
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.startsWith(geocode, "WM");
    }

    @Override
    @Nullable
    public String getGeocodeFromURI(@NonNull final Uri uri) {
        // coord.info URLs
        if (uri.getHost().equalsIgnoreCase("coord.info")) {
            final String topLevel = uri.getLastPathSegment();
            if (canHandle(topLevel)) {
                return topLevel;
            }
        }
        // waymarking URLs http://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon
        if (uri.getHost().equalsIgnoreCase(getHost()) && uri.getPath().startsWith("/waymarks/")) {
            final String waymark = StringUtils.substringBefore(uri.getLastPathSegment(), "_");
            return waymark != null && canHandle(waymark) ? waymark : null;
        }
        return null;
    }
}
