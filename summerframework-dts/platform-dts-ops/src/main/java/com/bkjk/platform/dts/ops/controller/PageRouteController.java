package com.bkjk.platform.dts.ops.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.bkjk.platform.dts.ops.vo.GlobalLogState;

@Controller
public class PageRouteController {

    @RequestMapping({"/dts/list"})
    public String dtsList(Model model) {
        model.addAttribute("GlobalLogStates", GlobalLogState.values());
        return "dts/list";
    }

    @RequestMapping({"/index"})
    public String index(Model model, HttpServletRequest request) {
        return "index";
    }

    @RequestMapping(value = "/signin", method = RequestMethod.GET)
    public String login(@RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "logout", required = false) String logout) {
        return "login";
    }

    @RequestMapping({"/", ""})
    public String welcome(Model model) {
        return "redirect:/index";
    }
}
