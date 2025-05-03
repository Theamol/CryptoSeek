package com.cryptoseek;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/ListFilesServlet")
public class ListFilesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        listFiles(response);
    }

    private void listFiles(HttpServletResponse response) throws IOException {
    	DbxClientV2 client = DropboxConnection.getClient();
        List<String> fileNames = new ArrayList<>();

        try {
            ListFolderResult result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    if (metadata instanceof FileMetadata && metadata.getName().endsWith(".enc")) {
                        fileNames.add(metadata.getName());
                    }
                }
                if (!result.getHasMore()) break;
                result = client.files().listFolderContinue(result.getCursor());
            }

            response.setContentType("application/json");
            response.getWriter().write(new Gson().toJson(fileNames));

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error listing files");
        }
    }
}