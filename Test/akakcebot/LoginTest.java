package akakcebot;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LoginTest {
    static WebDriver driver;
    static BOT bot;
    static WebDriverWait wait;

    @BeforeAll
    public static void initialize() throws InterruptedException {
        // Initializes WebDriver and BOT before all tests (Tüm testlerden önce WebDriver ve BOT başlatılır)
        bot = new BOT();
        driver = bot.getDriver();
        driver.manage().window().maximize();
    }

    @AfterAll
    public static void end() {
        // Quits the browser session after all tests (Tüm testlerden sonra tarayıcı oturumu kapatılır)
        bot.quit();
    }

    @AfterEach
    void cleanPopups() {
        // Closes cookie banner if exists (Çerez popup'ı varsa kapatır)
        bot.closeCookieBannerIfExists();
    }

    @Test
    void testLoginWithValidCredentials() throws InterruptedException {
        // Tests login with correct credentials (Doğru bilgilerle giriş testi)
        boolean result = bot.login("testmailtesting@gmail.com", "123456789Test");
        assertTrue(result);
    }

    @Test
    void testLoginWithIncorrectPassword() throws InterruptedException {
        // Tests login with incorrect password (Yanlış şifre ile giriş testi)
        boolean result = bot.login("testmailtesting@gmail.com", "wrong_password");
        WebElement alert = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#m-w > div > div.m-c")));
        assertNotNull(alert);
        assertFalse(result);
    }

    @Test
    void testLoginWithEmptyFields() throws InterruptedException {
        // Tests login with empty email and password fields (Boş alanlarla giriş testi)
        boolean result = bot.login("", "");
        WebElement alert = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#m-w > div")));
        assertNotNull(alert);
        assertFalse(result);
    }

    @Test
    void testRememberMeOption() throws InterruptedException {
        // Tests "remember me" functionality with cookies (Beni hatırla/cookie özelliğini test eder)
        bot.login("testmailtesting@gmail.com", "123456789Test");
        bot.setCookies();
        assertFalse(bot.getCookie().isEmpty());
        driver.quit();
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("https://www.akakce.com/");
        if(bot.getCookie()==null){
            for (Cookie cookie : bot.getCookie()) {
                driver.manage().addCookie(cookie);
            }
        }
        driver.navigate().refresh();
        WebElement userMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#HM_v8 > i > a")));
        assertTrue(userMenu.isDisplayed());
    }

    @Test
    void testPasswordInputMasked() {
        // Tests if password input field is masked (Şifre alanının gizli olup olmadığını test eder)
        driver.get("https://www.akakce.com/akakcem/giris/");
        WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lifp")));
        assertEquals("password", passwordField.getAttribute("type"));
    }

    @Test
    void testRedirectionAfterLogin() throws InterruptedException {
        // Tests if redirected to main page after login (Girişten sonra ana sayfaya yönlendirme testi)
        bot.login("testmailtesting@gmail.com", "123456789Test");
        assertTrue(driver.getCurrentUrl().contains("akakce.com"));
    }

    @Test
    void testRateLimiting() throws InterruptedException {
        // Tests rate limit after multiple failed login attempts (Çoklu hatalı giriş sonrası rate limit testi)
        int count = 0;
        while (true) {
            bot.login("testmailtesting@gmail.com", "wrong_password");
            WebElement warningMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#m-w > div > div.m-c > div > p")));
            String mesaj = warningMessage.getText();
            System.out.println("Found message: " + mesaj);
            if (mesaj.equals("Lütfen daha sonra tekrar deneyin.")){
                break;
            }
            count++;
        }
        // System.out.println("Rate Limit " + count);
        // Prints: Rate Limit <count>
        System.out.println("Rate Limit " + count); // (Rate Limit ...)
        assertTrue(true);
    }

    @Test
    void testLoginUsingEnterKey() {
        // Tests login action using Enter key (Enter tuşuyla giriş yapılmasını test eder)
        driver.get("https://www.akakce.com/akakcem/giris/");
        WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("life")));
        WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lifp")));
        emailField.sendKeys("testmailtesting@gmail.com");
        passwordField.sendKeys("123456789Test");
        passwordField.sendKeys(Keys.RETURN);
        wait.until(ExpectedConditions.urlContains("akakce.com"));
        assertTrue(driver.getCurrentUrl().startsWith("https://www.akakce.com"));
    }

    @Test
    void testLoginForNonExistAccount() throws InterruptedException {
        // Tests login attempt for a non-existent account (Olmayan bir hesap için giriş denemesi)
        boolean result = bot.login("nonexistmailfortesting@hotmail.com", "password");
        WebElement warningMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#m-w > div > div.m-c > div > p")));
        assertEquals("Bu e-postaya kayıtlı bir hesap bulunamadı.", warningMessage.getText());
        assertFalse(result);
    }
}

