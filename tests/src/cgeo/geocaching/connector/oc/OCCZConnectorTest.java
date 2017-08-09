package cgeo.geocaching.connector.oc;

import static org.assertj.core.api.Java6Assertions.assertThat;
import junit.framework.TestCase;

public class OCCZConnectorTest extends TestCase {

    public static void testGetGeocodeFromUrl() throws Exception {
        final OCCZConnector connector = new OCCZConnector();
        assertThat(connector.getGeocodeFromURL("http://opencaching.cz/viewcache.php?cacheid=610")).isEqualTo("OZ0262");
        assertThat(connector.getGeocodeFromURL("http://www.opencaching.de/viewcache.php?cacheid=151223")).isNull();
    }

}
