package com.wind.openmeetings.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
//		System.out.println(getServletConfig().getInitParameter("userConfig"));
		String file=getServletConfig().getInitParameter("userConfig");
//		String file="D:\\deliver\\release\\WebContent\\config\\users.xml";
		Map<String, String> userMap = XmlUtil.readUserList(file);
		String name = request.getParameter("name");
		String password = request.getParameter("password");
		if(name == null && password == null ) {
			System.out.println("failure");
		}
		if(userMap.containsKey(name) && userMap.get(name).equals(password)){
			HttpSession session = request.getSession(true); 
			session.setAttribute("name", name);
			session.setAttribute("password", password);
			response.sendRedirect("SchedualTask.jsp");
//			request.getRequestDispatcher("SchedualTask.jsp").forward(request, response);
		} else {
			//失败
			response.sendRedirect("login.jsp?flag=1");
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doPost(request, response);
	}
	
	public void sendMessage(String keywords) {
		System.out.println(keywords);
	}

}
