package akakcebot;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FollowUnfollowTest {
    static WebDriver driver;
    static BOT bot;
    static WebDriverWait wait;

    @BeforeEach
    public void initialize() throws InterruptedException {
        // Initializes driver and logs in before each test (Her testten önce driver başlatılır ve giriş yapılır)
        bot = new BOT();
        driver = bot.getDriver();
        wait=bot.getWait();
        driver.manage().window().maximize();
        bot.login("testmailtesting@gmail.com", "123456789Test");
        bot.unfollowall();
        Thread.sleep(1500);
    }

    @AfterEach
    public void end() {
        // Quits the driver after each test (Her testten sonra driver kapatılır)
        bot.quit();
    }

    // Tests follow feature for different products (Farklı ürünlerde takip özelliğini test eder)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung", "beko"})
    void testFollowItemWithDifferentInputs(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        bot.follow();

        WebElement profile = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#HM_v8 > i > a")));
        profile.click();
        WebElement followings = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#AL > li:nth-child(2) > a")));
        followings.click();

        WebElement list = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body > div.rw_v8 > div > form")));
        assertNotNull(list);
    }

    // Tests if duplicate follow shows a warning (Aynı ürünü iki kere takip edince uyarı çıkıyor mu kontrol eder)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    void testPreventDuplicateFollows(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        bot.follow();
        bot.follow();
        WebElement warning = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"m-w\"]/div/div[2]")));
        assertTrue(warning.isDisplayed());
    }

    // Tests if follow without login redirects to login page (Giriş yapılmadan takip denenirse login sayfasına yönlendirme test edilir)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    void testFollowRedirectsIfNotLoggedIn(String productName) throws InterruptedException {
        bot.logout();
        bot.searchProduct(productName);
        bot.follow();
        assertEquals("https://www.akakce.com/akakcem/giris/?s=34&t=8", driver.getCurrentUrl());
    }

    // Checks if followed product is in following list (Takip edilen ürün takip listesinde mi kontrol edilir)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    void testFollowedItemInList(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        bot.follow();
        bot.goFollowingList();
        Thread.sleep(500);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("fl-mc-i-n")));
        List<WebElement> items = driver.findElements(By.className("fl-mc-i-n"));
        boolean found = items.stream().anyMatch(item -> item.getText().toLowerCase().contains(productName.toLowerCase()));
        Thread.sleep(500);
        assertTrue(found);
    }

    // Checks follow button state after refresh (Sayfa yenilendiğinde buton durumu doğru mu kontrol edilir)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung", "beko"})
    void testFollowButtonStateAfterRefresh(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        bot.follow();
        driver.navigate().refresh();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement followBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("ufo_v8")));
        String classAttr = followBtn.getAttribute("class");
        assertTrue(classAttr.contains("a"));
    }

    // Tests following multiple products at once (Aynı anda birden fazla ürün takip etme testi)
    @ParameterizedTest
    @CsvSource({"iphone 13, 3", "beko, 2"})
    void testFollowMultipleItems(String productName, int count) throws InterruptedException {
        bot.searchProduct(productName);
        driver.navigate().refresh();
        bot.multifollow(count);
        bot.goFollowingList();
        Thread.sleep(500);
        List<WebElement> items = driver.findElements(By.xpath("//span[@class='fl-mc-i-n']"));
        int found = 0;
        for (WebElement item : items) {
            System.out.println("Takip edilen ürün: " + item.getText());
            if (item.getText().toLowerCase().contains(productName.toLowerCase())) {
                found++;
            }
        }
        System.out.println("Searched Product: " + productName + " | Found: " + found);
        assertTrue(found >= count);
    }

    // Checks if follows persist after logout/login (Çıkış yapıp tekrar girince takipler kayboluyor mu kontrol edilir)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13"})
    void testFollowPersistenceAcrossSession(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        Thread.sleep(500);
        bot.follow();
        Thread.sleep(500);
        bot.logout();
        Thread.sleep(500);
        bot.login("testmailtesting@gmail.com", "123456789Test");
        bot.goFollowingList();
        Thread.sleep(1500);
        List<WebElement> items = driver.findElements(By.xpath("//span[@class='fl-mc-i-n']"));
        boolean found=false;
        for (WebElement item : items) {
            System.out.println("Takip edilen ürün: " + item.getText());
            if (item.getText().toLowerCase().contains(productName.toLowerCase())) {
                found=true;
                break;
            }
        }
        assertTrue(found);
    }

    // Utility method for multi-follow (Çoklu takip testleri için yardımcı fonksiyon)
    void searchAndMultiFollow(String text, int count) throws InterruptedException {
        bot.searchProduct(text);
        bot.multifollow(count);
    }

    // Checks if follow limit popup is shown after 200+ follows (200'den fazla ürün takip edince limit popupı çıkıyor mu kontrol edilir)
    @Test
    void testFollowLimit() throws InterruptedException {
        String[] products = {"iphone 13", "iphone 14", "iphone 15", "beko", "samsung", "3060", "lego"};
        int perProduct = 30;
        for (String product : products) {
            searchAndMultiFollow(product, perProduct);
        }
        Thread.sleep(1000);
        WebElement limitscreen = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'en fazla 200')]")
        ));
        assertTrue(limitscreen.isDisplayed());
    }

    // Tests unfollowing a product (Bir ürünü takipten çıkarmayı test eder)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13"})
    void testUnfollowItem(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        bot.follow();
        bot.unfollow(productName);
        WebElement profile = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#HM_v8 > i > a")));
        profile.click();
        WebElement followings = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#AL > li:nth-child(2) > a")));
        followings.click();
        boolean found = false;
        try {
            WebElement edit = driver.findElement(By.cssSelector("#editBtn"));
            edit.click();
            List<WebElement> items = driver.findElements(By.className("fl-mc-i-n"));
            found = items.stream().noneMatch(item -> item.getText().toLowerCase().contains(productName.toLowerCase()));
        } catch (NoSuchElementException e) {
            // If edit button not found, no item is followed (Edit butonu yoksa takip edilen ürün yok demektir)
            found = true;
        }
        assertTrue(found);
    }

    // Tests unfollow action via popup (Bildirim popup'ı üzerinden takipten çıkma testi)
    @ParameterizedTest
    @ValueSource(strings = {"iphone 13"})
    void testUnfollowViaNotificationPopup(String productName) throws InterruptedException {
        bot.searchProduct(productName);
        bot.follow();
        driver.navigate().refresh();

        WebElement followBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ufo_v8.a")));
        followBtn.click();
        WebElement popup = driver.findElement(By.xpath("//*[contains(@class, 'ufo_v8') and contains(@class, 'a')]"));
        assertTrue(popup.isDisplayed());
    }
}

