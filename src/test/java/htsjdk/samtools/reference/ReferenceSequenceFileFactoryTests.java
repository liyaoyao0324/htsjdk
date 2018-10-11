package htsjdk.samtools.reference;

import htsjdk.HtsjdkTest;
import java.io.File;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Simple tests for the reference sequence file factory */
public class ReferenceSequenceFileFactoryTests extends HtsjdkTest {
  public static final File hg18 =
      new File(
          "src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.fasta");
  public static final File hg18bgzip =
      new File(
          "src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.fasta.gz");

  @Test
  public void testPositivePath() {
    final ReferenceSequenceFile f = ReferenceSequenceFileFactory.getReferenceSequenceFile(hg18);
    Assert.assertTrue(f instanceof AbstractFastaSequenceFile);
  }

  @Test
  public void testGetIndexedReader() {
    final ReferenceSequenceFile f =
        ReferenceSequenceFileFactory.getReferenceSequenceFile(hg18, true, true);
    Assert.assertTrue(
        f instanceof IndexedFastaSequenceFile,
        "Got non-indexed reader when expecting indexed reader.");
  }

  @Test
  public void testGetNonIndexedReader1() {
    final ReferenceSequenceFile f =
        ReferenceSequenceFileFactory.getReferenceSequenceFile(hg18, false, true);
    Assert.assertTrue(
        f instanceof FastaSequenceFile,
        "Got indexed reader when truncating at whitespace! FAI must truncate.");
  }

  @Test
  public void testGetNonIndexedReader2() {
    final ReferenceSequenceFile f =
        ReferenceSequenceFileFactory.getReferenceSequenceFile(hg18, true, false);
    Assert.assertTrue(
        f instanceof FastaSequenceFile, "Got indexed reader when requesting non-indexed reader.");
  }

  @Test
  public void testDefaultToIndexed() {
    final ReferenceSequenceFile f =
        ReferenceSequenceFileFactory.getReferenceSequenceFile(hg18, true);
    Assert.assertTrue(f instanceof IndexedFastaSequenceFile, "Got non-indexed reader by default.");
  }

  @Test
  public void testBlockCompressedIndexed() {
    final ReferenceSequenceFile f =
        ReferenceSequenceFileFactory.getReferenceSequenceFile(hg18bgzip, true);
    Assert.assertTrue(f instanceof BlockCompressedIndexedFastaSequenceFile);
  }

  @DataProvider
  public Object[][] canCreateIndexedFastaParams() {
    return new Object[][] {
      {hg18, true},
      {hg18bgzip, true},
      {
        new File(
            "src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.noindex.fasta"),
        false
      },
      {
        new File(
            "src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.noindex.fasta.gz"),
        false
      },
      {
        new File(
            "src/test/resources/htsjdk/samtools/reference/Homo_sapiens_assembly18.trimmed.nogzindex.fasta.gz"),
        false
      }
    };
  }

  @Test(dataProvider = "canCreateIndexedFastaParams")
  public void testCanCreateIndexedFastaReader(final File path, final boolean indexed) {
    Assert.assertEquals(
        ReferenceSequenceFileFactory.canCreateIndexedFastaReader(path.toPath()), indexed);
  }

  @DataProvider
  public Object[][] fastaNames() {
    return new Object[][] {
      {"break.fa", "break.dict"},
      {"break.txt.txt", "break.txt.dict"},
      {"break.fasta.fasta", "break.fasta.dict"},
      {"break.fa.gz", "break.dict"},
      {"break.txt.gz.txt.gz", "break.txt.gz.dict"},
      {"break.fasta.gz.fasta.gz", "break.fasta.gz.dict"}
    };
  }

  @Test(dataProvider = "fastaNames")
  public void testGetDefaultDictionaryForReferenceSequence(
      final String fastaFile, final String expectedDict) throws Exception {
    Assert.assertEquals(
        ReferenceSequenceFileFactory.getDefaultDictionaryForReferenceSequence(new File(fastaFile)),
        new File(expectedDict));
  }
}
