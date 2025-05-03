package com.cryptoseek;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/session-check")
public class SessionCheckServlet extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || (session.getAttribute("email") == null && session.getAttribute("userEmail") == null)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
}