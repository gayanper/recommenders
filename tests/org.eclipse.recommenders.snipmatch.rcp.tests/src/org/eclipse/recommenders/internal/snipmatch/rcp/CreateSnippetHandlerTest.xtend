package org.eclipse.recommenders.internal.snipmatch.rcp

import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jface.text.TextSelection
import org.eclipse.recommenders.internal.snipmatch.rcp.CreateSnippetHandler
import org.eclipse.recommenders.snipmatch.Snippet
import org.eclipse.recommenders.testing.CodeBuilder
import org.eclipse.recommenders.testing.jdt.JavaProjectFixture
import org.junit.Test

import static org.junit.Assert.*

class CreateSnippetHandlerTest {

    static val fixture = new JavaProjectFixture(ResourcesPlugin.getWorkspace(), "test")
    CharSequence code

    Snippet actual

    @Test
    def void testNewArrayAndCalls() {
        code = CodeBuilder::method(
            '''
                $List[] l[] = new List[0][];
                l.hashCode();$
            ''')
        exercise()

        assertEquals(
            '''
                List[] ${l:newName(array)}[] = new List[0][];
                ${l}.hashCode();
                ${:import(java.util.List)}${cursor}
            '''.toString,
            actual.code
        )
    }
        /*
         * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=439984
         */
        @Test
    def void testNoJavaLangImport() {
        code = CodeBuilder::method(
            '''
                $String s = null;$
            ''')
        exercise()

        assertEquals(
            '''
                String ${s:newName(java.lang.String)} = null;
                ${cursor}
            '''.toString,
            actual.code
        )
    }

    @Test
    def void testNoJavaLangImportButOtherImports() {
        code = CodeBuilder::method(
            '''
                $String s = null;$
                $List l = null;$
            ''')
        exercise()

        assertEquals(
            '''
                String ${s:newName(java.lang.String)} = null;
                List ${l:newName(java.util.List)} = null;
                ${:import(java.util.List)}${cursor}
            '''.toString,
            actual.code
        )
    }

    /*
     * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=439330
     */
    @Test
    def void testNoEmptyImport() {
        code = CodeBuilder::method(
            '''
                $int two = 1 + 1;$
            ''')
        exercise()

        assertEquals(
            '''
                int ${two:newName(int)} = 1 + 1;
                ${cursor}
            '''.toString,
            actual.code
        )
    }

    @Test
    def void testGenerics() {
        code = CodeBuilder::method(
            '''
                $HashMap<Set, List> map = null$;
            ''')
        exercise()

        assertEquals(
            '''
                HashMap<Set, List> ${map:newName(java.util.HashMap)} = null
                ${:import(java.util.HashMap, java.util.List, java.util.Set)}${cursor}
            '''.toString,
            actual.code
        )
    }


    def void exercise() {
        val struct = fixture.createFileAndParseWithMarkers(code)
        val cu = struct.first;
        val start = struct.second.head;
        val end = struct.second.last;
        val editor = EditorUtility.openInEditor(cu) as CompilationUnitEditor;
        editor.selectionProvider.selection = new TextSelection(start, end - start)
        val sut = new CreateSnippetHandler(newHashSet())
        actual = sut.createSnippet(editor)
    }
}