[![Maven](https://img.shields.io/maven-central/v/ch.astorm/jotlmsg.svg)](https://search.maven.org/search?q=g:ch.astorm%20AND%20a:jotlmsg)
[![Build](https://secure.travis-ci.org/ctabin/jotlmsg.png)](http://travis-ci.org/ctabin/jotlmsg)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/ctabin/jotlmsg.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/ctabin/jotlmsg/context:java)

# jotlmsg
It's a simple API meant to easily generate Microsoft Outlook message files (.msg). 
This library is based on [Apache POI](https://poi.apache.org) and is a 100% Java implementation.

## Installation

Simply add the ```jotlmsg.jar``` and its dependencies to your classpath.

If you're using maven, then simply add the following dependency:
```xml
<dependency>
    <groupId>ch.astorm</groupId>
    <artifactId>jotlmsg</artifactId>
    <version>1.7</version>
</dependency>
```

If you're intending to use the ```toMimeMessage()``` methods, then you also
need the following dependency:
```xml
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>
```

## Usage examples

Create a new message:
```Java
OutlookMessage message = new OutlookMessage();
message.setSubject("Hello");
message.setPlainTextBody("This is a message draft.");

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

Managing attachments:
```Java
OutlookMessage message = new OutlookMessage();
message.addAttachment("aFile.txt", "text/plain", new FileInputStream("data.txt")); //will be stored in memory
message.addAttachment("aDocument.pdf", "application/pdf", new FileInputStream("file.pdf")); //will be stored in memory
message.addAttachment("hugeFile.zip", "application/zip", a -> new FileInputStream("data.zip")); //piped to output stream

List<OutlookMessageAttachment> attachments = message.getAttachments();
```

## Limitations

The current implementation allows to create simple msg files with many recipients (up to 2048) and attachments (up to 2048). 
However, there is not current support of Microsoft Outlook advanced features like appointments or calendar integration, nor embedded messages.

Unfortunately, only plain text messages are supported. It is not possible to inject HTML for now.
