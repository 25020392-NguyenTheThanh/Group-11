import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;

public class FxmlLoaderTest {
    @BeforeAll
    public static void initJfx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    @Test
    public void testLoadBidderAuctionList() throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(FxmlLoaderTest.class.getResource("/com/example/group11/bidderAuctionList-view.fxml"));
            loader.load();
            System.out.println("--- SUCCESS: loaded bidderAuctionList-view.fxml successfully ---");
        } catch (Exception e) {
            System.err.println("--- FAILURE: failed to load bidderAuctionList-view.fxml ---");
            e.printStackTrace();
            throw e;
        }
    }
}
