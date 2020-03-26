package org.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/a")
    @ResponseBody
    public ModelMap get(){
        ModelMap modelMap = new ModelMap();
        return modelMap;
    }

}
