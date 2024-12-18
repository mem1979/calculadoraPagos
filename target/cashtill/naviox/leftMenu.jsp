<%-- 
NaviOX. Navigation and security for OpenXava applications.
--%>

<%@ page import="org.openxava.util.Users"%>
<%@ page import="com.openxava.naviox.util.NaviOXPreferences"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Arrays"%>

<%@ include file="../xava/imports.jsp" %>

<jsp:useBean id="modules" class="com.openxava.naviox.Modules" scope="session"/>

<%
boolean isFirstSteps = com.openxava.naviox.Modules.FIRST_STEPS.equals(modules.getCurrentModuleName());
String display = isFirstSteps ? "class='ox-display-block-important'" : "";

// Lista de módulos que deseas ocultar (usa los nombres exactos de los módulos como "starh/Cuil", "starh/DatosPersonales")
List<String> modulosOcultos = Arrays.asList(
		"cashtill/Denominaciones",
		"cashtill/CajaEntrada",
		"cashtill/CajaSalida",
		"cashtill/MovimientoCaja",
		"cashtill/Caja",
		"cashtill/CajaDashboard"
		);

// Obtenemos la lista de módulos
List<?> listaModulos = modules.getAll(request);
%>

<div id="modules_list" <%= display %>>  

    <div id="modules_list_top"> 

        <div id="application_title">
        
            <div id="application_name">
                <%= modules.getApplicationLabel(request) %>
            </div>
        
            <div id="organization_name">
                <% String organizationName = modules.getOrganizationName(request); %>
                <%= organizationName %>
                <%@ include file="organizationNameExt.jsp" %>
            </div>
        
        </div>
        
        <% if (Users.getCurrent() != null && modules.showsIndexLink()) { %>
             
            <a href="<%= request.getContextPath() %>/m/Index">
                <div id='organizations_index' class='<%= "Index".equals(request.getParameter("module")) ? "selected" : "" %>'>    
                    <i class="mdi mdi-apps"></i>
                    <xava:label key="myOrganizations" />
                </div>    
            </a>
            
        <% } %>
    
        <jsp:include page="selectModules.jsp">
            <jsp:param name="bookmarkModules" value="true" />
        </jsp:include>
        
        <% if (modules.showsSearchModules(request)) { %>
        <div id="search_modules">
            <input id="search_modules_text" type="text" size="38" placeholder='<xava:message key="search_modules" />' />
        </div>
        <% } %>
        
    </div>     
    
    <div id="modules_list_outbox">
        <table id="modules_list_box">
            <tr id="modules_list_content">
                <td>
                    <ul>
                    <%
                    // Iteramos sobre cada módulo y lo imprimimos si no está en la lista de ocultos
                    for (Object moduleObj : listaModulos) {
                        String moduleName = moduleObj.toString().replace("MetaModule: ", "");

                        // Verificamos si el módulo no está en la lista de ocultos
                        if (!modulosOcultos.contains(moduleName)) {
                            // Dividimos el nombre del módulo y tomamos solo la parte después de la barra "/"
                            String simpleModuleName = moduleName.contains("/") ? moduleName.split("/")[1] : moduleName;
                    %>
                        <li>
                            <a href="<%= request.getContextPath() %>/m/<%= moduleName %>">
                               <xava:label key="<%= simpleModuleName %>" /> <!-- Usamos la clave i18n --> 
                          	</a>
                        </li>
                        <% }
                    }
                    %>
                    </ul>
                </td>                        
            </tr>
        </table>
    </div>
    
</div>

<% if (!isFirstSteps) { %>
    <a id="modules_list_hide">
        <i class="mdi mdi-chevron-left"></i>
    </a>
    
    <a id="modules_list_show">
        <i class="mdi mdi-chevron-right"></i>
    </a>
<% } %>
