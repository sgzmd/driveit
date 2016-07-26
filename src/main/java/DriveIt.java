
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class DriveIt {
  /**
   * Application name.
   */
  private static final String APPLICATION_NAME =
      "Drive API Java Quickstart";

  private static final String FOLDER_NAME = "DriveIt";

  /**
   * Directory to store user credentials for this application.
   */
  private static final java.io.File DATA_STORE_DIR = new java.io.File(
      System.getProperty("user.home"), ".credentials/drive-java-quickstart.json");

  /**
   * Global instance of the {@link FileDataStoreFactory}.
   */
  private static FileDataStoreFactory DATA_STORE_FACTORY;

  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY =
      JacksonFactory.getDefaultInstance();

  /**
   * Global instance of the HTTP transport.
   */
  private static HttpTransport HTTP_TRANSPORT;

  private static final String READAB_URL = "https://readability.com/api/content/v1/parser?token=<insert token here>&url=https://hbr.org/2007/09/investigative-negotiation";

  /**
   * Global instance of the scopes required by this quickstart.
   * <p>
   * If modifying these scopes, delete your previously saved credentials
   * at ~/.credentials/drive-java-quickstart.json
   */
  private static final List<String> SCOPES =
      Arrays.asList(DriveScopes.DRIVE);

  static {
    try {
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Creates an authorized Credential object.
   *
   * @return an authorized Credential object.
   * @throws IOException
   */
  public static Credential authorize() throws IOException {
    // Load client secrets.
    InputStream in =
        DriveIt.class.getResourceAsStream("/client_id.json");
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(DATA_STORE_FACTORY)
            .setAccessType("offline")
            .build();
    Credential credential = new AuthorizationCodeInstalledApp(
        flow, new LocalServerReceiver()).authorize("user");
    System.out.println(
        "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
    return credential;
  }

  /**
   * Build and return an authorized Drive client service.
   *
   * @return an authorized Drive client service
   * @throws IOException
   */
  public static Drive getDriveService() throws IOException {
    Credential credential = authorize();
    return new Drive.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  public static void main(String[] args) throws IOException, ParseException {
    // Build a new authorized API client service.
    Drive service = getDriveService();

    // Print the names and IDs for up to 10 files.
    FileList result = service.files().list()
        .setFields("nextPageToken, files(id, name)")
        .execute();
    List<File> files = result.getFiles();

    if (files == null || files.size() == 0) {
      System.out.println("No files found.");
      return;
    } else {
      for (File f : files) {
        if (f.getName().equals(FOLDER_NAME)) {
          System.out.println("Folder is there");
          String json = IOUtils.toString(new URL(READAB_URL), "UTF-8");
          JSONParser parser = new JSONParser();
          JSONObject obj = (JSONObject) parser.parse(json);
          String content = (String) obj.get("content");
          Path tempFile = Files.createTempFile("myfile", ".html");
          content = "<html><body>" + content + "</body></html>";
          IOUtils.write(content, new OutputStreamWriter(new FileOutputStream(tempFile.toFile())));

          String title = obj.get("title") + ".html";
          FileContent fc = new FileContent("text/html", tempFile.toFile());
          File fileMetadata = new File();
          fileMetadata.setName(title);
          fileMetadata.setMimeType("text/html");

          service.files().create(fileMetadata, fc).execute();

          return;
        }
      }
    }

    service.files().create(new File().setMimeType("application/vnd.google-apps.folder").setName(FOLDER_NAME)).execute();
  }

}