package codeu.controller;

import codeu.model.data.Conversation;
import codeu.model.data.Message;
import codeu.model.data.user.User;
import codeu.model.data.user.UserGroup;
import codeu.model.store.basic.ConversationStore;
import codeu.model.store.basic.MessageStore;
import codeu.model.store.basic.UserStore;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests {@link ProfileServlet}
 *
 * @author Elle Tojaroon (etojaroo@codeustudents.com)
 */
public class ProfileServletTest {

    private static final String MESSAGE_CREATION_TIME_ISO = "2018-12-04T02:44:50.00Z";
    private static final String CONVERSATIONS_REQUEST_ATTRIBUTE = "conversations";
    private static final String OWNER_REQUEST_ATTRIBUTE = "owner";
    private static final String MESSAGE_DISPLAY_REQUEST_ATTRIBUTE =
        "messageDisplayTimeToMessageContent";
    private static final String DESCRIPTION_REQUEST_PARAM = "description";
    private static final String USER_SESSION_ATTRIBUTE = "user";

    private ProfileServlet mockProfileServlet;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private RequestDispatcher mockRequestDispatcher;
    private ConversationStore mockConversationStore;
    private MessageStore mockMessageStore;
    private UserStore mockUserStore;
    private HttpSession mockSession;

    private static String getCurrentTimeZone() {
        return TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT);
    }

    @Before
    public void setup() {
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
        mockRequestDispatcher = Mockito.mock(RequestDispatcher.class);
        mockConversationStore = Mockito.mock(ConversationStore.class);
        mockMessageStore = Mockito.mock(MessageStore.class);
        mockUserStore = Mockito.mock(UserStore.class);

        // partial mock ProfileServlet because
        // ProfileServlet.doPost() makes a call to ProfileServlet.doPost()
        mockProfileServlet = Mockito.mock(ProfileServlet.class);
        Mockito.doCallRealMethod()
            .when(mockProfileServlet)
            .setConversationStore(mockConversationStore);
        Mockito.doCallRealMethod()
            .when(mockProfileServlet)
            .setMessageStore(mockMessageStore);
        Mockito.doCallRealMethod()
            .when(mockProfileServlet)
            .setUserStore(mockUserStore);
        mockProfileServlet.setConversationStore(mockConversationStore);
        mockProfileServlet.setMessageStore(mockMessageStore);
        mockProfileServlet.setUserStore(mockUserStore);

        mockSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession()).thenReturn(mockSession);
        Mockito.when(mockRequest.getRequestDispatcher("/WEB-INF/view/profile.jsp"))
            .thenReturn(mockRequestDispatcher);
    }

    @Test
    public void testDoGet_ValidUser() throws IOException, ServletException {
        mockUser();
        mockConversation();

        callDoGet();

        Mockito.verify(mockRequest)
            .setAttribute(Mockito.eq(CONVERSATIONS_REQUEST_ATTRIBUTE),
                Mockito.any());
        Mockito.verify(mockRequest)
            .setAttribute(Mockito.eq(OWNER_REQUEST_ATTRIBUTE),
                Mockito.any());
        Mockito.verify(mockRequest)
            .setAttribute(
                Mockito.eq(MESSAGE_DISPLAY_REQUEST_ATTRIBUTE),
                Mockito.any());
        Mockito.verify(mockRequestDispatcher)
            .forward(mockRequest, mockResponse);
    }

    private void mockUser() {
      String username = "Mary";
      User user =
          new User(
              UUID.randomUUID(),
              username,
              username,
              Instant.now(),
              UserGroup.REGULAR_USER);
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + username);
      Mockito.when(mockUserStore.getUser(username))
          .thenReturn(user);
    }

    private void mockConversation() {
        Conversation conversation =
            new Conversation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "mock conversation title",
                Instant.now()
            );
        List<Conversation> conversations = new ArrayList<>();
        conversations.add(conversation);
        Mockito.when(mockConversationStore.getAllConversations())
            .thenReturn(conversations);
    }

    private Message mockMessage(User user, Conversation conversation) {
        Message message =
            new Message(
                UUID.randomUUID(),
                conversation.getId(),
                user.getId(),
                "some message content",
                Instant.parse(MESSAGE_CREATION_TIME_ISO)
            );
        List<Message> messages = new ArrayList<>(Arrays.asList(message));
        Mockito
            .when(mockMessageStore.getMessagesInConversation(conversation.getId()))
            .thenReturn(messages);
        return message;
    }

    private void callDoGet() throws IOException, ServletException {
        Mockito.doCallRealMethod()
            .when(mockProfileServlet)
            .doGet(mockRequest, mockResponse);
        mockProfileServlet.doGet(mockRequest, mockResponse);
    }

    @Test
    public void testDoGet_UserNotFound() throws IOException, ServletException {
      String invalidUsername = "inValidUsername";
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + invalidUsername);

      callDoGet();

      Mockito.verify(mockResponse)
          .sendRedirect("/conversations");
    }

    @Test
    public void testDoPost_OwnerEdit() throws IOException, ServletException {
      User mockOwner = Mockito.mock(User.class);
      String ownerName = "ownerName";
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + ownerName);
      Mockito.when(mockSession.getAttribute(USER_SESSION_ATTRIBUTE))
          .thenReturn(ownerName);
      Mockito.when(mockUserStore.getUser(ownerName))
          .thenReturn(mockOwner);
      String newDescription = "I'm Olaf. I like warm hugs.";
      String cleanedNewDescription = "I'm Olaf. I like warm hugs.";
      Mockito.when(mockRequest.getParameter(DESCRIPTION_REQUEST_PARAM))
          .thenReturn(newDescription);
      Mockito.when(mockUserStore.updateUserDescription(mockOwner, cleanedNewDescription))
          .thenReturn(true);

      callDoPost();
      Mockito.verify(mockOwner).setDescription(cleanedNewDescription);
      Mockito.verify(mockResponse).sendRedirect("/profile/" + ownerName);
    }

    @Test
    public void testDoPost_UserNotLoggedIn() throws IOException, ServletException {
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + "ownerName");
      Mockito.when(mockSession.getAttribute(USER_SESSION_ATTRIBUTE))
          .thenReturn(null);

      callDoPost();
      Mockito.verify(mockResponse).sendRedirect("/login");
    }

    @Test
    public void testDoPost_UserNotOwner() throws IOException, ServletException {
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + "ownerName");
      Mockito.when(mockSession.getAttribute("user"))
          .thenReturn("notOwnerName");

      callDoPost();
      Mockito.verify(mockResponse).sendRedirect("/login");
    }

    @Test
    public void testDoPost_InvalidUser() throws IOException, ServletException {
      String ownerName = "ownerName";
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + ownerName);
      Mockito.when(mockSession.getAttribute(USER_SESSION_ATTRIBUTE))
          .thenReturn(ownerName);
      Mockito.when(mockUserStore.getUser(ownerName))
          .thenReturn(null);

      callDoPost();
      Mockito.verify(mockResponse).sendRedirect("/login");
    }

    @Test
    public void testDoPost_CleansHtmlContent() throws IOException, ServletException {
      User mockOwner = Mockito.mock(User.class);
      String ownerName = "ownerName";
      Mockito.when(mockRequest.getRequestURI())
          .thenReturn("/profile/" + ownerName);
      Mockito.when(mockSession.getAttribute(USER_SESSION_ATTRIBUTE))
          .thenReturn(ownerName);
      Mockito.when(mockUserStore.getUser(ownerName))
          .thenReturn(mockOwner);
      String newDescription =
          "<p>I'm <strong>Olaf</strong>.</p></br></br>"
          + "<script>JavaScript</script>"
          + "<div> I like warm hugs.</div>";
      String cleanedNewDescription = "I'm Olaf. I like warm hugs.";
      Mockito.when(mockRequest.getParameter(DESCRIPTION_REQUEST_PARAM))
          .thenReturn(newDescription);
      Mockito.when(mockUserStore.updateUserDescription(mockOwner, cleanedNewDescription))
          .thenReturn(true);

      callDoPost();
      Mockito.verify(mockOwner).setDescription(cleanedNewDescription);
      Mockito.verify(mockResponse).sendRedirect("/profile/" + ownerName);
    }

  @Test
  public void testDoPost_UpdateDescriptionFails() throws IOException, ServletException {
    User mockOwner = Mockito.mock(User.class);
    String ownerName = "ownerName";
    Mockito.when(mockRequest.getRequestURI())
        .thenReturn("/profile/" + ownerName);
    Mockito.when(mockSession.getAttribute(USER_SESSION_ATTRIBUTE))
        .thenReturn(ownerName);
    Mockito.when(mockUserStore.getUser(ownerName))
        .thenReturn(mockOwner);
    String newDescription = "I'm Olaf. I like warm hugs.";
    String cleanedNewDescription = "I'm Olaf. I like warm hugs.";
    Mockito.when(mockRequest.getParameter(DESCRIPTION_REQUEST_PARAM))
        .thenReturn(newDescription);
    Mockito.when(mockUserStore.updateUserDescription(mockOwner, cleanedNewDescription))
        .thenReturn(false);

    callDoPost();
    Mockito.verify(mockOwner).setDescription(cleanedNewDescription);
    Mockito.verify(mockResponse, Mockito.never()).sendRedirect("/profile/" + ownerName);
    Mockito.verify(mockRequest).setAttribute("updateDescriptionError",
        "Failed to update your description. Please try again later.");
    Mockito.verify(mockProfileServlet).doGet(mockRequest, mockResponse);
  }

  private void callDoPost() throws IOException, ServletException {
      Mockito.doCallRealMethod()
          .when(mockProfileServlet)
          .doPost(mockRequest, mockResponse);
      mockProfileServlet.doPost(mockRequest, mockResponse);
  }
}
