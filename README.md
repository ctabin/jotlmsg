[![Maven](https://img.shields.io/maven-central/v/ch.astorm/jotlmsg.svg)](https://search.maven.org/search?q=g:ch.astorm%20AND%20a:jotlmsg)
[![Build](https://app.travis-ci.com/ctabin/jotlmsg.svg?branch=master)](https://app.travis-ci.com/github/ctabin/jotlmsg/branches)
[![Javadoc](https://javadoc.io/badge2/ch.astorm/jotlmsg/javadoc.svg)](https://javadoc.io/doc/ch.astorm/jotlmsg) 

# jotlmsg
It's a simple API meant to easily generate Microsoft Outlook message files (.msg). 
This library is based on [Apache POI](https://poi.apache.org) and is a 100% Java implementation.

Here the compatibility map of this API:

| Version | JDK                 | Package
| ------- | ------------------- | ---------
| <= 1.9  | JDK 8 and upwards   | `javax` 
| >= 2.0  | JDK 11 and upwards  | `jakarta`
| >= 3.0  | JDK 21 and upwards  | `jakarta`

## Installation

Simply add the `jotlmsg.jar` and its dependencies to your classpath.

If you're using maven, then simply add the following dependency:
```xml
<dependency>
    <groupId>ch.astorm</groupId>
    <artifactId>jotlmsg</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Usage examples

Create a new message:
```Java
OutlookMessage message = new OutlookMessage();
message.setSubject("Hello");

//plain text body or html body
message.setPlainTextBody("This is a plain text body.");
message.setHtmlBody("<html><body><p>This is an html body with <strong>bold</strong> and <i>italic</i> styles.</p></body></html>");

//creates a new Outlook Message file
message.writeTo(new File("myMessage.msg"));

//creates a javax.mail MimeMessage
MimeMessage mimeMessage = message.toMimeMessage();
```

Read an existing message:
```Java
OutlookMessage message = new OutlookMessage(new File("aMessage.msg"));
System.out.println(message.getSubject());
System.out.println(message.getPlainTextBody());
```

Managing recipients:
```Java
OutlookMessage message = new OutlookMessage();
message.addRecipient(Type.TO, "cedric@jotlmsg.com");
message.addRecipient(Type.TO, "bill@microsoft.com", "Bill");
message.addRecipient(Type.CC, "steve@apple.com", "Steve");
message.addRecipient(Type.BCC, "john@gnu.com");
        
List<OutlookMessageRecipient> toRecipients = message.getRecipients(Type.TO);
List<OutlookMessageRecipient> ccRecipients = message.getRecipients(Type.CC);
List<OutlookMessageRecipient> bccRecipients = message.getRecipients(Type.BCC);
List<OutlookMessageRecipient> allRecipients = message.getAllRecipients();
```

Managing optional replyto recipients:
```Java
OutlookMessage message = new OutlookMessage();
message.setReplyTo(List.of("reply1@jotlmsg.com", "reply2@jotlmsg.com"));

List<String> replyToRecipients = message.getReplyTo();
```

Managing attachments:
```Java
OutlookMessage message = new OutlookMessage();
message.addAttachment("aFile.txt", "text/plain", new FileInputStream("data.txt")); //will be stored in memory
message.addAttachment("aDocument.pdf", "application/pdf", new FileInputStream("file.pdf")); //will be stored in memory
message.addAttachment("hugeFile.zip", "application/zip", a -> new FileInputStream("data.zip")); //piped to output stream

List<OutlookMessageAttachment> attachments = message.getAttachments();
```

### HTML message with inlined attachments

The APIs allow to generate a message with embedded picture inside an HTML body that
will then be editable directly in Outlook. The inlined attachment can be referenced
by using the `cid:` prefix in a tag.

```java
//generate a unique content id to be referenced
String contentId = UUID.randomUUID().toString();

OutlookMessage message = new OutlookMessage();
message.setSubject("My HTML message");
message.setFrom("sender@jotlmsg.com");
message.addRecipient(Type.TO, "cedric@jotlmsg.com", "Cédric");

//optional alternative plain text message that can be shown if HTML is not displayed
message.setPlainTextBody("Alternative plain text message");

//defines the htmlbody with injected content id reference
String htmlBody = """
                  <html>
                    <body>
                      <div>
                        Inline attached smiley:
                        <img src="cid:%s" alt="Smiley">
                      </div>
                    </body>
                  </html>
                  """;
message.setHtmlBody(String.format(htmlBody, contentId));

//adds the inlined attachment with the referenced content id
OutlookMessageAttachment inlineAttachment = new OutlookMessageAttachment("Face-smile.png", "image/png", a -> OutlookMessageMIMETest.class.getResourceAsStream("Face-smile.png"));
inlineAttachment.setContentId(contentId);
message.addAttachment(inlineAttachment);
```

**Note:** The inlined attachements are not shown as "real" attachment in Outlook. It's an alternative to external URLs that
might be prevented to be loaded by the mail client of the final recipient.

## Limitations

The current implementation allows to create simple msg files with many recipients (up to 2048) and attachments (up to 2048). 
However, there is no current support of Microsoft Outlook advanced features like appointments or calendar integration, nor embedded messages.

## Donate

This project is completely developed during my spare time.

Since I'm a big fan of cryptocurrencies and especially [Cardano](https://cardano.org) (ADA), you can send me
some coins at the address below (check it [here](https://cardanoscan.io/address/addr1q9sgms4vc038nq7hu4499yeszy0rsq3hjeu2k9wraksle8arg0n953hlsrtdzpfnxxw996l4t6qu5xsx8cmmakjcqhksaqpj66)):

```
addr1q9sgms4vc038nq7hu4499yeszy0rsq3hjeu2k9wraksle8arg0n953hlsrtdzpfnxxw996l4t6qu5xsx8cmmakjcqhksaqpj66
```

