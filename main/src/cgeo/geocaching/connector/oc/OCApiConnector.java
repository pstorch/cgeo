package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.DisposableHandler;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Callable;

import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;

public class OCApiConnector extends OCConnector implements ISearchByGeocode {

    // Levels of Okapi we support
    // oldapi is around rev 500
    // current is from rev 798 onwards
    public enum ApiSupport {
        oldapi,
        current
    }

    // Levels of OAuth-Authentication we support
    public enum OAuthLevel {
        Level0,
        Level1,
        Level3
    }

    private final String cK;
    private final ApiSupport apiSupport;
    private final String licenseString;

    public OCApiConnector(@NonNull final String name, @NonNull final String host, final boolean https, final String prefix, final String cK, final String licenseString, final ApiSupport apiSupport, @NonNull final String abbreviation) {
        super(name, host, https, prefix, abbreviation);
        this.cK = cK;
        this.apiSupport = apiSupport;
        this.licenseString = licenseString;
    }

    public void addAuthentication(final Parameters params) {
        if (StringUtils.isBlank(cK)) {
            throw new IllegalStateException("empty OKAPI OAuth token for host " + getHost() + ". fix your keys.xml");
        }
        final String rotCK = CryptUtils.rot13(cK);
        // check that developers are not using the Ant defined properties without any values
        if (StringUtils.startsWith(rotCK, "${")) {
            throw new IllegalStateException("invalid OKAPI OAuth token '" + rotCK + "' for host " + getHost() + ". fix your keys.xml");
        }
        params.put(CryptUtils.rot13("pbafhzre_xrl"), rotCK);
    }

    @Override
    @NonNull
    public String getLicenseText(@NonNull final Geocache cache) {
        // NOT TO BE TRANSLATED
        return "© " + cache.getOwnerDisplayName() + ", <a href=\"" + getCacheUrl(cache) + "\">" + getName() + "</a>, " + licenseString;
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final DisposableHandler handler) {
        final Geocache cache = OkapiClient.getCache(geocode);
        if (cache == null) {
            return null;
        }
        return new SearchResult(cache);
    }

    @Override
    public boolean isActive() {
        // currently always active, but only for details download
        return true;
    }

    public OAuthLevel getSupportedAuthLevel() {
        return OAuthLevel.Level1;
    }

    public String getCK() {
        return CryptUtils.rot13(cK);
    }

    public String getCS() {
        return StringUtils.EMPTY;
    }

    public ApiSupport getApiSupport() {
        return apiSupport;
    }

    public int getTokenPublicPrefKeyId() {
        return 0;
    }

    public int getTokenSecretPrefKeyId() {
        return 0;
    }

    /**
     * Checks if a search based on a user name targets the current user
     *
     * @param username
     *            Name of the user the query is searching after
     * @return True - search target and current is same, False - current user not known or not the same as username
     */
    public boolean isSearchForMyCaches(final String username) {
        return false;
    }

    @Override
    @Nullable
    public String getGeocodeFromURI(@NonNull final Uri uri) {
        final String shortHost = getShortHost();

        final String geocodeFromId = getGeocodeFromCacheId(uri, shortHost);
        if (geocodeFromId != null) {
            return geocodeFromId;
        }

        return super.getGeocodeFromURI(uri);
    }

    /**
     * get the OC1234 geocode from an internal cache id, for URLs like host.tld/viewcache.php?cacheid
     */
    @Nullable
    protected String getGeocodeFromCacheId(@NonNull final Uri uri, final String host) {
        if (!StringUtils.containsIgnoreCase(uri.getHost(), host)) {
            return null;
        }

        // host.tld/viewcache.php?cacheid=cacheid
        final String id = uri.getPath().startsWith("/viewcache.php") ? uri.getQueryParameter("cacheid") : "";
        if (StringUtils.isNotBlank(id)) {
            final String geocode = Maybe.fromCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    final String normalizedUrl = StringUtils.replaceIgnoreCase(uri.toString(), getShortHost(), getShortHost());
                    return OkapiClient.getGeocodeByUrl(OCApiConnector.this, normalizedUrl);
                }
            }).subscribeOn(AndroidRxUtils.networkScheduler).blockingGet();

            if (geocode != null && canHandle(geocode)) {
                return geocode;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        // mobile
        String url = OkapiClient.getMobileRegistrationUrl(this);
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        // non-mobile
        url = OkapiClient.getRegistrationUrl(this);
        if (StringUtils.isNotBlank(url)) {
            return url;
        }
        // fall back to a simple host name based pattern
        return super.getCreateAccountUrl();
    }

}
