package cgeo.geocaching.connector;

import cgeo.geocaching.models.Geocache;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

class UnknownConnector extends AbstractConnector {

    @Override
    @NonNull
    public String getName() {
        return "Unknown caches";
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        throw new IllegalStateException("no valid name for unknown connector");
    }

    @Override
    @Nullable
    public String getCacheUrl(@NonNull final Geocache cache) {
        return null;
    }

    @Override
    @NonNull
    public String getHost() {
        return StringUtils.EMPTY; // we have no host for these caches
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.isNotBlank(geocode);
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        throw new IllegalStateException("getCacheUrl cannot be called on unknown caches");
    }

    @Override
    @Nullable
    public String getGeocodeFromURI(@NonNull final Uri uri) {
        return null;
    }

}
