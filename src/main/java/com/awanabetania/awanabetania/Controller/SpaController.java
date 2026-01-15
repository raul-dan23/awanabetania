package com.awanabetania.awanabetania.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Aceasta clasa este "lipiciul" dintre Java si site-ul React.
 * Rezolva problema cand dai Refresh (F5) la pagina si apare eroare 404.
 */
@Controller
public class SpaController {

    /**
     * Aceasta metoda prinde orice adresa care nu este un fisier real (nu are punct in ea).
     * De exemplu: /login sau /dashboard nu sunt fisiere, sunt rute React.
     * Java le trimite inapoi la index.html ca sa se ocupe React de ele.
     */
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        // Trimitem totul catre pagina principala index.html
        return "forward:/index.html";
    }
}