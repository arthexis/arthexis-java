package com.arthexis.platform.app.admin.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

  @GetMapping
  public String view() {
    return "redirect:/admin/index.html";
  }
}
