package dev.motion.vk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@SpringBootApplication
public class Main {

  @Value("${vk.app.id}")
  private String vkAppId;

  @Value("${vk.app.key}")
  private String vkAppKey;

  @Value("${vk.app.redirectUri}")
  private String vkAppRedirectUri;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  @RequestMapping("/")
  ModelAndView index(@RequestParam(required = false) String code) throws Exception {
    if (code == null) {
      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl("https://oauth.vk.com/authorize");
      uriBuilder.queryParam("client_id", vkAppId);
      uriBuilder.queryParam("redirect_uri", vkAppRedirectUri);
      uriBuilder.queryParam("display", "page");
      uriBuilder.queryParam("response_type", "code");
      uriBuilder.queryParam("revoke", "1");
      uriBuilder.queryParam("v", "5.73");

      ModelAndView modelAndView = new ModelAndView("index");
      modelAndView.addObject("loginLink", uriBuilder.toUriString());
      return modelAndView;
    }

    ModelAndView modelAndView = new ModelAndView("login");
    modelAndView.addObject("userFullName", getUserFullName(getAccessToken(code)));
    return modelAndView;
  }

  private String getAccessToken(String code) throws Exception {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl("https://oauth.vk.com/access_token");
    uriBuilder.queryParam("client_id", vkAppId);
    uriBuilder.queryParam("client_secret", vkAppKey);
    uriBuilder.queryParam("redirect_uri", vkAppRedirectUri);
    uriBuilder.queryParam("code", code);

    String response = new RestTemplate().getForObject(uriBuilder.toUriString(), String.class);

    ObjectNode responseJson = new ObjectMapper().readValue(response, ObjectNode.class);

    if (!responseJson.has("access_token")) {
      throw new RuntimeException("Problem while getting access_token by code {" + code + "}: " + response);
    }

    return responseJson.get("access_token").asText();
  }

  private static String getUserFullName(String accessToken) throws Exception {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl("https://api.vk.com/method/users.get");
    uriBuilder.queryParam("v", "5.52");
    uriBuilder.queryParam("access_token", accessToken);

    String response = new RestTemplate().getForObject(uriBuilder.toUriString(), String.class);

    ObjectNode responseJson = new ObjectMapper().readValue(response, ObjectNode.class);

    String firstName = responseJson.get("response").get(0).get("first_name").asText();
    String lastName = responseJson.get("response").get(0).get("last_name").asText();

    return firstName + " " + lastName;
  }
}
