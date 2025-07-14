package akakcebot;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FilterTest {
    static WebDriver driver;
    static BOT bot;

    @BeforeEach
    public void initialize() throws InterruptedException {
        bot = new BOT();
        driver = bot.getDriver();
        driver.manage().window().maximize();
        driver.get("https://www.akakce.com/laptop-notebook.html");
    }

    @AfterEach
    public void end() {
        bot.quit();
    }

    // Price + Brand + Feature filter test (Fiyat + Marka + Özellik kombinasyonu testi)
    @ParameterizedTest
    @CsvSource({
            "40000,60000,c136934,c1163562,HP"
    })
    void test_Filter_With_Price_Brand_Feature(String priceMin, String priceMax, String brandId, String featureId, String productBrand) throws InterruptedException {
        bot.filterTest(
                "https://www.akakce.com/laptop-notebook.html",
                priceMin,
                priceMax,
                brandId,
                featureId,
                productBrand
        );
        List<WebElement> products = driver.findElements(By.xpath("//ul[@id='CPL']//li[@data-mk='" + productBrand + "']"));
        Assertions.assertFalse(products.isEmpty(), "No products found with filter!");
        for (WebElement item : products) {
            Assertions.assertEquals(productBrand, item.getAttribute("data-mk"), "Wrong brand!");
        }
    }

    // Brand only filter test (Sadece marka ile filtreleme testi)
    @ParameterizedTest
    @CsvSource({"'','','c136934','','HP'"})
    void test_Filter_With_Brand_Only(String priceMin, String priceMax, String brandId, String featureId, String productBrand) throws InterruptedException {
        bot.filterTest(
                "https://www.akakce.com/laptop-notebook.html",
                priceMin,
                priceMax,
                brandId,
                featureId,
                productBrand
        );
        List<WebElement> products = driver.findElements(By.xpath("//ul[@id='CPL']//li[@data-mk='" + productBrand + "']"));
        Assertions.assertFalse(products.isEmpty(), "No products found with selected brand!");
        for (WebElement item : products) {
            Assertions.assertEquals(productBrand, item.getAttribute("data-mk"), "Wrong brand!");
        }
    }

    // Price only filter test (Sadece fiyat ile filtreleme testi)
    @ParameterizedTest
    @CsvSource({"40000,60000,'','',''"})
    void test_Filter_With_Price_Only(String priceMin, String priceMax, String brandId, String featureId, String productBrand) throws InterruptedException {
        bot.filterTest("https://www.akakce.com/laptop-notebook.html", priceMin, priceMax, brandId, featureId, productBrand);
        List<WebElement> products = driver.findElements(By.xpath("//ul[@id='CPL']//li"));
        Assertions.assertFalse(products.isEmpty(), "No products found with price filter!");
    }

    // No result expected filter test (Geçersiz filtre ile ürün bulunamama testi)
    @ParameterizedTest
    @CsvSource({"1,2,c136934,c1163562,HP"})
    void test_Filter_No_Result(String priceMin, String priceMax, String brandId, String featureId, String productBrand) throws InterruptedException {
        bot.filterTest("https://www.akakce.com/laptop-notebook.html", priceMin, priceMax, brandId, featureId, productBrand);
        Thread.sleep(2000);
        List<WebElement> products = driver.findElements(By.xpath("//ul[@id='CPL']//li[@data-mk='" + productBrand + "']"));
        Thread.sleep(2000);
        Assertions.assertTrue(products.isEmpty(), "No products should be listed!");
    }
}
