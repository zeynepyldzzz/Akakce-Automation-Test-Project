package akakcebot;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCompRedirectTest {
    static WebDriver driver;
    static BOT bot;

    @BeforeEach
    public void initialize() throws InterruptedException {
        bot = new BOT();
        driver = bot.getDriver();
        driver.manage().window().maximize();
        driver.get("https://www.akakce.com");
    }

    @AfterEach
    public void end() {
        bot.quit();
    }

    // Test if logged-in user can see the price comparison table (Giriş yapmış kullanıcı fiyat karşılaştırmayı görebiliyor mu)
    @Test
    public void testLoggedInUserSeesPriceComparison() throws InterruptedException {
        bot.login("testmailtesting@gmail.com", "123456789Test");
        Thread.sleep(500);
        bot.gotoDetail("iphone 13"); // Go to product detail (Ürün detayına git)
        WebElement priceList = driver.findElement(By.id("PL"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", priceList);
        Thread.sleep(1000);
        List<WebElement> moreBtns = driver.findElements(By.id("SAP"));
        if (!moreBtns.isEmpty() && moreBtns.get(0).isDisplayed()) {
            ((JavascriptExecutor) driver).executeScript(
                    "let el = document.querySelector('efilli-layout-dynamic'); if(el) el.style.display='none';"
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", moreBtns.get(0));
            Thread.sleep(1500);
        }
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(1000);
        List<WebElement> priceSpans = driver.findElements(By.xpath("//span[contains(@class,'pt_v8')]"));
        System.out.println("Total price span count: " + priceSpans.size());
        assertTrue(priceSpans.size() > 1);
        for (WebElement price : priceSpans) {
            if (!price.isDisplayed()) {
                System.out.println("Skipped invisible price: " + price.getText());
                continue;
            }
            assertTrue(price.isDisplayed());
            assertFalse(price.getText().isBlank());
        }
    }

    // Test if guest user can see the price comparison table (Çıkış yapmış kullanıcı fiyat karşılaştırmayı görebiliyor mu)
    @Test
    public void testGuestUserSeesPriceComparison() throws InterruptedException {
        bot.logout();
        bot.gotoDetail("iphone 13");
        WebElement priceList = bot.waitForAndFind(By.id("PL"));
        assertTrue(priceList.isDisplayed());
    }

    // Test if prices are sorted in ascending order (Fiyatlar artan şekilde mi)
    @Test
    public void testPricesAreAscending() throws InterruptedException {
        bot.gotoDetail("iphone 13");
        List<WebElement> sellerList = driver.findElements(By.xpath("//ul[@id='PL']/li[contains(@class,'c')]"));
        List<Double> priceList = new ArrayList<>();
        for (WebElement li : sellerList) {
            List<WebElement> priceSpans = li.findElements(By.xpath(".//span[contains(@class,'pt_v8')]"));
            if (priceSpans.isEmpty()) continue;
            String priceText = priceSpans.get(0).getText();
            priceText = priceText.replaceAll("[^\\d,\\.]", "").replace(".", "").replace(",", ".");
            if (!priceText.isBlank()) {
                double price = Double.parseDouble(priceText);
                priceList.add(price);
            }
        }
        for (int i = 1; i < priceList.size(); i++) {
            assertTrue(priceList.get(i) >= priceList.get(i - 1));
        }
    }

    // Test if seller links are correct (Satıcı linkleri doğru mu)
    @Test
    public void testCorrectSellerLinks() throws InterruptedException {
        bot.gotoDetail("iphone 13");
        List<WebElement> sellerLinks = driver.findElements(By.xpath("//ul[@id='PL']//a[contains(@class,'iC') and contains(@class,'xt_v8')]"));
        assertFalse(sellerLinks.isEmpty());
        String originalTab = driver.getWindowHandle();
        WebElement link = sellerLinks.get(0);
        ((JavascriptExecutor) driver).executeScript("window.open(arguments[0].href, '_blank');", link);
        Thread.sleep(1500);
        Set<String> handles = driver.getWindowHandles();
        String newTab = null;
        for (String handle : handles) {
            if (!handle.equals(originalTab)) {
                newTab = handle;
                break;
            }
        }
        assertNotNull(newTab);
        driver.switchTo().window(newTab);
        String currentUrl = driver.getCurrentUrl();
        assertNotNull(currentUrl);
        assertTrue(currentUrl.startsWith("http"));
        driver.close();
        driver.switchTo().window(originalTab);
    }


    // Test if "Free Shipping" label is shown (Ücretsiz kargo etiketi var mı)
    @Test
    public void testFreeShippingLabel() throws InterruptedException {
        bot.gotoDetail("iphone 13");
        List<WebElement> sellerLis = driver.findElements(By.xpath("//ul[@id='PL']/li[contains(@class,'c')]"));
        boolean found = false;
        for (WebElement li : sellerLis) {
            List<WebElement> freeShipping = li.findElements(By.xpath(".//em[contains(@class,'uk_v8')]"));
            for (WebElement em : freeShipping) {
                if (em.getText().toLowerCase().contains("kargo")) {
                    assertTrue(em.isDisplayed());
                    found = true;
                }
            }
        }
        assertTrue(found);
    }

    // Test if prices have correct currency format (Fiyatlarda para birimi doğru mu)
    @Test
    public void testPricesCurrencyFormat() throws InterruptedException {
        bot.gotoDetail("iphone 13");
        List<WebElement> priceSpans = driver.findElements(By.xpath("//ul[@id='PL']//span[contains(@class,'pt_v8')]"));
        assertFalse(priceSpans.isEmpty());
        for (WebElement price : priceSpans) {
            String text = price.getText() + price.getAttribute("innerHTML"); // sometimes <i> inside (bazen <i> içinde)
            assertTrue(text.contains("TL") || text.contains("$") || text.contains("€"),"Incorrect or missing currency: " + text);
        }
    }

    // Test if seller link opens in a new tab (Satıcı linki yeni sekmede açılıyor mu)
    @Test
    public void testSellerLinkOpensNewTab() throws InterruptedException {
        bot.gotoDetail("iphone 13");
        List<WebElement> sellerLinks = driver.findElements(By.xpath("//ul[@id='PL']//a[contains(@class,'iC') and contains(@class,'xt_v8')]"));
        assertFalse(sellerLinks.isEmpty());
        String originalHandle = driver.getWindowHandle();
        bot.Redirect(sellerLinks.get(0));
        Thread.sleep(1500);
        Set<String> handles = driver.getWindowHandles();
        String newTab = null;
        for (String handle : handles) {
            if (!handle.equals(originalHandle)) {
                newTab = handle;
                break;
            }
        }
        assertNotNull(newTab);

        driver.switchTo().window(newTab);
        String url = driver.getCurrentUrl();
        assertTrue(url.startsWith("http"));
        driver.close();
        driver.switchTo().window(originalHandle);
    }
}
