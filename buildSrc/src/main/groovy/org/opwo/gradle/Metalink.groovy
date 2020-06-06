import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.apache.http.client.utils.URIBuilder

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.security.DigestInputStream
import java.text.SimpleDateFormat
import java.security.MessageDigest

class Metalink extends DefaultTask {

    @Input
    String fileSet
    String url
    String outputFile

    def generateMD5(File file) {
        file.withInputStream {
            new DigestInputStream(it, MessageDigest.getInstance('MD5')).withStream {
                it.eachByte {}
                it.messageDigest.digest().encodeHex() as String
            }
        }
    }

    @TaskAction
    def generateLinks() {
        if (url == null) {
            url = project.properties["serverFilesUrl"]
        }
        println(url)

        def xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        def metalinkNode = xml.createElementNS("urn:ietf:params:xml:metalinkNode:metalink","metalink")
        xml.appendChild(metalinkNode)

        SimpleDateFormat dateFormat = new SimpleDateFormat('dd-MMM-yyyy')
        def publishedNode = xml.createElement("published")
        def publishedNodeText = xml.createTextNode(dateFormat.format(new Date()))
        publishedNode.appendChild(publishedNodeText)
        metalinkNode.appendChild(publishedNode)

        def current_directory = new File(fileSet)
        println(current_directory.getAbsolutePath())

        current_directory.traverse(type: groovy.io.FileType.FILES){ file ->
            println(file)
            def fileNode = xml.createElement("file")
            fileNode.setAttribute("name",file.name)

            def sizeNode = xml.createElement("size")
            def fileSize = Long.toString(file.length())
            def sizeTextNode = xml.createTextNode(fileSize)
            sizeNode.appendChild(sizeTextNode)
            fileNode.appendChild(sizeNode)

            def hashNode = xml.createElement("hash")
            hashNode.setAttribute("type","md5")
            def fileHash = generateMD5(file)
            def hashTextNode = xml.createTextNode(fileHash)
            hashNode.appendChild(hashTextNode)
            fileNode.appendChild(hashNode)

            def urlNode = xml.createElement("url")
            def filePath = current_directory.toURI().relativize(file.toURI()).toString()
            def fileUrl = new URL(url.toURL(), filePath)
            def urlTextNode = xml.createTextNode(fileUrl.toString())
            urlNode.appendChild(urlTextNode)
            fileNode.appendChild(urlNode)

            metalinkNode.appendChild(fileNode)
        }


        // write file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(xml);
        StreamResult streamResult = new StreamResult(new File(outputFile));


        transformer.transform(domSource, streamResult);


        println(xml)

    }
}