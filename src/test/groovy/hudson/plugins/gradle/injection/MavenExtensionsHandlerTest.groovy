package hudson.plugins.gradle.injection

import hudson.FilePath
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MavenExtensionsHandlerTest extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    @Subject
    MavenExtensionsHandler mavenExtensionsHandler = new MavenExtensionsHandler()

    @Unroll
    def "only copies extension if it doesn't exist"() {
        given:
        def folder = tempFolder.newFolder()
        def root = new FilePath(folder)

        when:
        def firstFilePath = extensionCopier.call(root)

        then:
        firstFilePath.exists()
        def firstLastModified = firstFilePath.lastModified()
        firstLastModified > 0

        when:
        def secondFilePath = extensionCopier.call(root)

        then:
        secondFilePath.exists()
        secondFilePath.remote == firstFilePath.remote
        def secondLastModified = firstFilePath.lastModified()
        secondLastModified == firstLastModified

        where:
        extensionCopier << [
            { FilePath filePath -> new MavenExtensionsHandler().copyExtensionToAgent(filePath) },
            { FilePath filePath -> new MavenExtensionsHandler().copyCCUDExtensionToAgent(filePath) }
        ]
    }

    def "removes all files"() {
        given:
        def folder = tempFolder.newFolder()
        def root = new FilePath(folder)

        when:
        def geExtensionFilePath = mavenExtensionsHandler.copyExtensionToAgent(root)
        def ccudExtensionFilePath = mavenExtensionsHandler.copyCCUDExtensionToAgent(root)

        then:
        geExtensionFilePath.exists()
        ccudExtensionFilePath.exists()

        geExtensionFilePath.getParent().remote == ccudExtensionFilePath.getParent().remote

        when:
        mavenExtensionsHandler.deleteAllExtensionsFromAgent(root)

        then:
        !geExtensionFilePath.exists()
        !ccudExtensionFilePath.exists()
    }
}
