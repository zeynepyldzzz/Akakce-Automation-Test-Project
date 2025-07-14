package akakcebot;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class BOT {
    // Main login page URL (Ana giriş sayfası adresi)
    private final String loginUrl = "https://www.akakce.com/akakcem/giris/";
    private final int waitTimeMS =200;
    private final int TIMEOUT = 25;
    private int followed =0;
    private WebDriver driver;
    private final WebDriverWait wait;
    private final Actions actions;
    private Set<Cookie> cookieSet;

    public BOT() {
        // Initializes WebDriver, WebDriverWait and Actions (WebDriver, WebDriverWait ve Actions nesnelerini başlatır)
        this.driver = new ChromeDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
        this.actions = new Actions(driver);
    }

    public int getFollowed() {
        // Returns the number of followed items (Takip edilen ürün sayısını döner)
        return followed;
    }
    public WebDriverWait getWait() {
        return wait;
    }
    public WebDriver getDriver() {
        return driver;
    }
    public Set<Cookie> getCookie(){
        return cookieSet;
    }
    public void setCookies(){
        // Adds saved cookies to the driver (Kayıtlı çerezleri driver'a ekler)
        for (Cookie cookie : cookieSet){
            driver.manage().addCookie(cookie);
        }
    }
    public WebElement waitForAndFind(By selector) {
        // Waits for an element to appear and returns it (Bir elementin görünmesini bekler ve döner)
        return wait.until(ExpectedConditions.presenceOfElementLocated(selector));
    }

    void handleTabs() throws InterruptedException {
        // Switches to new browser tab (Yeni sekmeye geçer)
        String mainTab = driver.getWindowHandle();
        Thread.sleep(waitTimeMS);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(mainTab)) {
                driver.switchTo().window(handle);
                break;
            }
        }
    }

    void closeCookieBannerIfExists() {
        // Closes cookie popup if exists (Çerez popup'ı varsa kapatır)
        try {
            WebElement popupBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#m-w .alertX .buttons button")));
            if (popupBtn.isDisplayed()) {
                popupBtn.click();
            }
        } catch (Exception ignored) { }
    }

    public boolean login(String email, String password) {
        // Attempts login with provided credentials (Girilen bilgilerle giriş yapmayı dener)
        boolean success = false;
        driver.get(loginUrl);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("life")));
            emailField.clear();
            emailField.sendKeys(email);

            WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lifp")));
            passwordField.clear();
            passwordField.sendKeys(password);

            // Optionally: Remember Me (Opsiyonel: Beni hatırla)
            // WebElement remember = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#lifr")));
            // if (!remember.isSelected()) remember.click();
            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("lfb")));
            submitButton.click();
            // Checks if login is successful via user menu (Kullanıcı menüsü üzerinden girişin başarılı olup olmadığını kontrol eder)
            try {
                WebElement userMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#HM_v8 > i > a")));
                if (userMenu != null && userMenu.isDisplayed()) {
                    System.out.println("Login successful! User menu found."); // (Giriş başarılı! Kullanıcı menüsü bulundu.)
                    success = true;
                }
            } catch (TimeoutException ex) {
                System.out.println("Login failed: Menu not found."); // (Giriş başarısız: Menü bulunamadı.)
            }
        } catch (Exception e) {

            System.out.println("Login failed or fields not found: " + e.getMessage()); // (Giriş başarısız veya alanlar bulunamadı)
        }
        return success;
    }

    public void searchProduct(String productName) {
        // Searches for a product by name (Ürün adına göre arama yapar)
        WebElement searchInput = waitForAndFind(By.id("q"));
        searchInput.clear();
        searchInput.sendKeys(productName + Keys.ENTER);
        String encodedName = productName.replace(" ", "+");
        wait.until(ExpectedConditions.urlContains(encodedName));
    }

    public void goFollowingList() throws InterruptedException {
        // Navigates to following list and opens edit (Takip edilenler listesine gider ve düzenlemeyi açar)
        WebElement profile = waitForAndFind(By.cssSelector("#HM_v8 > i > a"));
        profile.click();
        Thread.sleep(waitTimeMS);
        WebElement followings = waitForAndFind(By.cssSelector("#AL > li:nth-child(2) > a"));
        followings.click();
        Thread.sleep(waitTimeMS);
        WebElement edit = waitForAndFind(By.cssSelector("#editBtn"));
        edit.click();
    }

    public void follow() {
        // Clicks follow button for first listed product (İlk listelenen ürünü takip eder)
        WebElement followBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#APL > li:nth-child(1) > a > span > span.ufo_v8")));
        followBtn.click();
        System.out.println("followed"); // (takip edildi)
        followed++;
    }

    public void multifollow(int count) throws InterruptedException {
        // Follows multiple products in a row (Arka arkaya birden fazla ürünü takip eder)
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int i = 0; i < count ; i++) {
            // Checks follow limit popup before clicking (Tıklamadan önce takip limiti popup'ı var mı kontrol eder)
            if (isFollowLimitPopupVisible()) {
                closeFollowLimitPopup();
                System.out.println("Follow limit popup seen, stopping. Added product: " + i); // (Takip limiti popup’ı görüldü, takip işlemi bitti. Eklenen ürün: ...)
                break;
            }

            Thread.sleep(waitTimeMS);

            WebElement follow;
            try {
                follow = driver.findElement(By.cssSelector("#APL > li:nth-child(" + (i + 1) + ") > a > span > span.ufo_v8"));
                follow.click();
            } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                System.out.println("Popup prevented click, breaking loop."); // (Popup engelledi, tıklanamadı, döngü sonlandırılıyor.)
                break;
            }

            Thread.sleep(waitTimeMS);

            // Checks follow limit popup again (Yine takip limiti kontrolü)
            if (isFollowLimitPopupVisible()) {
                closeFollowLimitPopup();
                System.out.println("Follow limit popup seen, stopping. Added product: " + (i + 1)); // (Takip limiti popup’ı görüldü, takip işlemi bitti. Eklenen ürün: ...)
                break;
            }
            System.out.println("followed " + (i + 1)); // (takip edildi ...)
            followed++;

            js.executeScript("window.scrollBy(0,250)");
        }
    }

    // Checks if follow limit popup is visible (Takip limiti popup'ı açık mı kontrol eder)
    private boolean isFollowLimitPopupVisible() {
        List<WebElement> popups = driver.findElements(By.xpath("//*[contains(text(),'en fazla 200')]"));
        return !popups.isEmpty() && popups.get(0).isDisplayed();
    }

    // Closes follow limit popup if visible (Takip limiti popup'ı varsa kapatır)
    private void closeFollowLimitPopup() {
        try {
            WebElement closeBtn = driver.findElement(By.cssSelector("#m-w .alertX .buttons button"));
            closeBtn.click();
            Thread.sleep(300); // Waits for popup to close (Popup'ın kapanmasını bekler)
        } catch (Exception e) {

            System.out.println("Popup could not be closed or was already closed."); // (Popup kapatılamadı veya zaten kapalı.)
        }
    }

    public void unfollow(String itemtext) throws  InterruptedException{
        // Unfollows a given item by name (Belirli bir ürünü takipten çıkarır)
        try{
            goFollowingList();
            Thread.sleep(waitTimeMS);
            closeCookieBannerIfExists();
            Thread.sleep(waitTimeMS);
            List<WebElement> items = driver.findElements(By.className("fl-mc-i"));
            for (WebElement item : items) {
                if(item.findElement(By.className("fl-mc-i-n")).getText().toLowerCase().contains(itemtext)){
                    WebElement checkbox = item.findElement(By.className("fl-li-chk"));
                    checkbox.click();
                    Thread.sleep(waitTimeMS);
                    break;
                }
            }
            WebElement click= waitForAndFind(By.cssSelector("#deleteItemModalButton"));
            click.click();
            Thread.sleep(waitTimeMS);
            WebElement conf = waitForAndFind(By.cssSelector("#m-w > div > div.m-c > div > div > button:nth-child(2)"));
            conf.click();
            Thread.sleep(waitTimeMS);
        }
        catch (Exception e){
            System.out.println("No item to unfollow"); // (Takipten çıkarılacak ürün yok)
        }
        driver.get("https://www.akakce.com/");
    }

    public void unfollowall() throws InterruptedException {
        // Unfollows all items in the following list (Takip edilen tüm ürünleri takipten çıkarır)
        try{
            goFollowingList();
            Thread.sleep(waitTimeMS);
            closeCookieBannerIfExists();
            WebElement button = waitForAndFind(By.cssSelector("#allSelectBtn"));
            button.click();
            Thread.sleep(waitTimeMS);
            WebElement click= waitForAndFind(By.cssSelector("#deleteItemModalButton"));
            click.click();
            Thread.sleep(waitTimeMS);
            WebElement conf = waitForAndFind(By.cssSelector("#m-w > div > div.m-c > div > div > button:nth-child(2)"));
            conf.click();
            Thread.sleep(waitTimeMS);
        }
        catch (Exception e){
            driver.get("https://www.akakce.com/");
            System.out.println("No item to unfollow"); // (Takipten çıkarılacak ürün yok)
        }
    }

    public void gotoDetail(String item) throws InterruptedException {
        // Goes to the detail page of a product (Bir ürünün detay sayfasına gider)
        try {
            searchProduct(item);
            closeCookieBannerIfExists();
            Thread.sleep(waitTimeMS);
            WebElement firstProduct = waitForAndFind(By.className("bt_v8"));
            firstProduct.click();
        }
        catch (Exception e) {
            System.out.println("gotoDetail error: " + e.getMessage());
            throw e; // or return;
        }
    }

    public void Redirect(WebElement link) throws InterruptedException {
        // Redirects to vendor detail page in new tab (Vendor detayına yeni sekmede yönlendirir)
        Thread.sleep(waitTimeMS);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,800)");
        Thread.sleep(waitTimeMS);
        WebElement vendorButton = waitForAndFind(By.cssSelector("#PL > li.c > a > span.bt_v8"));
        vendorButton.click();
        handleTabs();
        System.out.println(driver.getCurrentUrl());
    }

    public void logout() throws InterruptedException {
        // Logs out from current user session (Şu anki oturumdan çıkış yapar)
        try {
            WebElement profileIcon = waitForAndFind(By.id("H_a_v8"));
            if (profileIcon.isDisplayed()) {
                Actions actions = new Actions(driver);
                actions.moveToElement(profileIcon).perform();
                Thread.sleep(500);
                WebElement exit = waitForAndFind(By.xpath("//*[@id=\"HM_v8\"]/ul/li[6]/a"));
                exit.click();
                Thread.sleep(waitTimeMS);
                driver.get("https://www.akakce.com/");
            }
        } catch (Exception e) {
            driver.get("https://www.akakce.com/");
            System.out.println("Logout already done or profile icon not found. Exception: " + e.getMessage());
        }
    }

    public void filterTest(String url, String priceMin, String priceMax, String brandId, String featureId, String productBrand) throws InterruptedException {
        // Tests filtering options on product list page (Ürün listesinde filtreleme seçeneklerini test eder)
        driver.get(url);
        Thread.sleep(waitTimeMS);
        if (priceMin != null && !priceMin.isEmpty() && priceMax != null && !priceMax.isEmpty()) {
            WebElement priceMinField = waitForAndFind(By.id("pf1"));
            WebElement priceMaxField = waitForAndFind(By.id("pf2"));
            Thread.sleep(waitTimeMS);
            priceMinField.clear();
            priceMinField.sendKeys(priceMin);
            priceMaxField.clear();
            priceMaxField.sendKeys(priceMax);
            WebElement applyButton = waitForAndFind(By.cssSelector("#FF_v9 > span.fpl_v9 > span > button"));
            applyButton.click();
            Thread.sleep(waitTimeMS);
        }
        if (brandId != null && !brandId.isEmpty()) {
            WebElement brandFilter = waitForAndFind(By.xpath("//input[@id='" + brandId + "']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", brandFilter);
            if (brandFilter.isEnabled() && brandFilter.isDisplayed()) {
                try {
                    brandFilter.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", brandFilter);
                }
            }
            Thread.sleep(waitTimeMS);
        }

        // Feature filter (Özellik filtresi)
        if (featureId != null && !featureId.isEmpty()) {
            WebElement featuresFilter = waitForAndFind(By.xpath("//input[@id='" + featureId + "']"));
            if (featuresFilter.isEnabled() && featuresFilter.isDisplayed()) {
                try {
                    featuresFilter.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", featuresFilter);
                }
            }
            Thread.sleep(waitTimeMS);
        }

        // Finds products and prints product count (Ürünleri bulur ve ürün sayısını yazdırır)
        List<WebElement> productElements;
        if (productBrand != null && !productBrand.isEmpty()) {
            productElements = driver.findElements(By.xpath("//ul[@id='CPL']//li[@data-mk='" + productBrand + "']"));
            System.out.println(productBrand + " brand product count: " + productElements.size()); // (xxx marka ürün sayısı: ...)
        } else {
            productElements = driver.findElements(By.xpath("//ul[@id='CPL']//li"));
            System.out.println("All product count: " + productElements.size()); // (Tüm ürünlerin sayısı: ...)
        }
    }

    public void quit() {
        // Ends browser session (Tarayıcı oturumunu sonlandırır)
        if (actions != null) {
            actions.pause(Duration.ofSeconds(TIMEOUT)).perform();
        }
        if (driver != null) {
            driver.quit();
        }
    }

}