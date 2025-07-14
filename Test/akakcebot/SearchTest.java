package akakcebot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.*;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
class SearchTest {
    static WebDriver driver;
    static BOT bot;

    @BeforeAll
    public static void initiliaze() throws InterruptedException {
        // Initializes WebDriver and BOT before all tests (Tüm testlerden önce WebDriver ve BOT başlatılır)
        bot=new BOT();
        driver = bot.getDriver();
        driver.manage().window().maximize();
        driver.get("https://www.akakce.com");
    }

    @AfterAll
    public static void end(){
        // Quits the WebDriver after all tests (Tüm testler bittikten sonra WebDriver kapatılır)
        bot.quit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    public void testValidSearchQuery(String productName) throws InterruptedException {
        // Tests search with a valid product name (Geçerli ürün adı ile arama testi)
        boolean succes = false;
        bot.searchProduct(productName);
        Thread.sleep(2000);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        for(WebElement result : results) {
            String title = result.getAttribute("title");
            System.out.println("Title: " + title);
            if(title != null && title.toLowerCase().contains(productName)) {
                succes = true;
                break;
            }
        }
        assertTrue(succes, productName+" must be found");
    }

    @Test
    public void testEmptySearchQuery() throws InterruptedException {
        // Tests search with an empty query string (Boş arama ile test)
        bot.searchProduct(" ");
        Thread.sleep(2000);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        assertTrue(results.size() > 0, "Products should arrive on empty search");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@", "@#$%^&*\""})
    public void testSpecialCharacterQuery() throws InterruptedException {
        // Tests search using only special characters (Sadece özel karakterlerle arama testi)
        bot.searchProduct("@#$%^&*");
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        assertEquals(0, results.size(), "The product should not come in a special character");
    }

    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    public void testCaseInsensitivity(String productName) throws InterruptedException {
        // Tests case insensitivity in search queries (Büyük/küçük harf duyarsızlığı testi)
        bot.searchProduct(productName);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        boolean found = results.stream().anyMatch(el -> el.getText().toLowerCase().contains(productName));
        assertTrue(found, productName+" should also come with a capital letter");
    }

    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    public void testPartialMatch(String partial) throws InterruptedException {
        // Tests partial keyword search functionality (Kısmi anahtar kelime arama testi)
        bot.searchProduct(partial);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        boolean found=false;
        for(WebElement result : results) {
            String title = result.getAttribute("title");
            System.out.println("Title: " + title);
            if(title != null && title.toLowerCase().contains(partial)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Product must come in partial matching");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Galaxyİphone 992", "FKLHJASBJKFAkjb"})
    public void testNonExistingProduct(String invalidProduct) throws InterruptedException {
        // Tests search with a non-existing product (Olmayan ürün adıyla arama testi)
        bot.searchProduct(invalidProduct);
        Thread.sleep(500);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        assertEquals(0, results.size(), "There should be no result on a product that does not exist");
    }

    @ParameterizedTest
    @ValueSource(strings = {"iphone 13", "samsung"})
    public void testSpecialCharacterHandling(String productName) throws InterruptedException {
        // Tests handling of special characters like dash (Tire gibi özel karakterlerle arama testi)
        try {
            bot.searchProduct(productName.replace(' ', '-'));
        }
        catch (Exception e){
            bot.searchProduct(productName);
        }
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        String normalizedProduct = productName.replace(" ", "").replace("-", "").toLowerCase();
        boolean found = results.stream().anyMatch(el ->
                el.getText().replace(" ", "").replace("-", "").toLowerCase().contains(normalizedProduct)
        );
        assertTrue(found, "The product should also appear in the hyphenated search");//(Ürün tireli aramada da gelmeli)
    }

    @Test
    public void testDropdownSuggestions() throws InterruptedException {
        // Tests dropdown search suggestions under searchbox (Arama kutusu altında öneri testi)
        bot.searchProduct("iph");
        List<WebElement> suggestions = driver.findElements(By.cssSelector(("body > div.rw_v8.search_v8 > p:nth-child(2)")));
        assertTrue(suggestions.size() > 0, "Suggestions should come under the search box");
    }

    @Test
    public void testLongSearchQuery() throws InterruptedException {
        // Tests search with a very long query string (Çok uzun arama sorgusu testi)
        String longQuery = "a".repeat(955);
        bot.searchProduct(longQuery);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        assertEquals(0, results.size(), "The product should not take too long to arrive");
    }

    @ParameterizedTest
    @CsvSource({"ipohne 13,iphone 13"})
    public void testTypoTolerance(String wrongProductName, String validProductName) throws InterruptedException {
        // Tests tolerance to typo in search (Yazım hatasına tolerans testi)
        bot.searchProduct(wrongProductName);
        List<WebElement> results = driver.findElements(By.cssSelector("#APL > li > a.pw_v8"));
        boolean found = results.stream().anyMatch(el -> el.getText().toLowerCase().contains(validProductName));
        assertTrue(found, "In case of a typo, the correct product should still arrive");
    }
    @ParameterizedTest
    @ValueSource(strings = {"hp", "samsung"})
    public void brandSearch(String productName){ // Searches by the entered brand name and tests whether there is only that brand
        // (Girilen marka adına göre arama yapar ve sadece o markanın olup olmadığını test eder)
        bot.searchProduct(productName);
        List<WebElement> listItems = driver.findElements(By.cssSelector("[data-mk]"));
        boolean found = true;
        int count =0;
        for (WebElement item : listItems) {
            count++;
            System.out.println(item.getAttribute("data-mk") +' '+count);
            if (!(item.getAttribute("data-mk").equalsIgnoreCase(productName))) {
                found = false;
                break;
            }
        }
        assertTrue(found, "Wanted brand (" + productName + ") not found.");
    }
}