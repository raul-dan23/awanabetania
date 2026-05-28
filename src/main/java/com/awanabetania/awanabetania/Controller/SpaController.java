package com.awanabetania.awanabetania.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Catch-all controller that enables client-side routing for the React SPA.
 * Any request path that does not contain a dot (i.e. is not a static asset request)
 * is forwarded to {@code index.html}, allowing React Router to handle the route.
 * Without this, a hard refresh (F5) on a React route would return a 404 from Spring.
 */
@Controller
public class SpaController {

    /**
     * Forwards all non-asset requests to the React entry point.
     *
     * @return forward directive to {@code index.html}
     */
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
