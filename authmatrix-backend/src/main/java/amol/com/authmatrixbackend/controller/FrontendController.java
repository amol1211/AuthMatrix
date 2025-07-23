package amol.com.authmatrixbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FrontendController {

    @RequestMapping(value = {
        "/",                     // root
        "/{x:[\\w\\-]+}",        // /about, /dashboard
        "/{x:^(?!api$).*$}/**/{y:[\\w\\-]+}" // nested routes, avoid /api/*
    })
    public String forward() {
        return "forward:/index.html";
    }
}
