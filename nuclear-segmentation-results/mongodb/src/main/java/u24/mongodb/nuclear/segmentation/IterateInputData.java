package u24.mongodb.nuclear.segmentation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;

/**
 * This iterates over the list of input data files.
 */
class InputFileListIterator implements Iterator<File> {
    BufferedReader reader;

    InputFileListIterator(BufferedReader myReader) {
        reader = myReader;
    }

    @Override
    public boolean hasNext() {
        try {
            return reader.ready();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public File next() {
        try {
            String currLine = reader.readLine();
            if (currLine != null) {
                return new File(currLine);
            } else {
                return null;
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported!");
    }

}

public class IterateInputData implements Iterator<String> {
    Iterator<File> iter;

    public IterateInputData(String inpData) {
        File inpFile = new File(inpData);
        if (inpFile.isDirectory()) {
            iter = FileUtils.iterateFiles(inpFile,
                    TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        } else {
            try {
                BufferedReader br = new BufferedReader(new FileReader(inpData));
                iter = new InputFileListIterator(br);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public String next() {
        return iter.next().getPath();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported!");
    }

}
