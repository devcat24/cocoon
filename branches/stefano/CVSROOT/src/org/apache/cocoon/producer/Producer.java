package org.apache.cocoon.producer;

import java.io.*;
import org.w3c.dom.*;
import javax.servlet.http.*;
import org.apache.cocoon.framework.*;

/**
 * This interface must be implemented by the classes that produce documents
 * encapsulating resources. These resource wrappers provide a way to call, 
 * execute or otherwise access the data contained or generated by these
 * resources and transform it into data models that can be used
 * by the publishing framework for futher processing.
 *
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version $Revision: 1.1.1.1 $ $Date: 1999-11-09 01:51:12 $
 */

public interface Producer extends Actor, Changeable {
    
    /**
     * This method is responsible to provide an input stream to read
     * the data generated or contained by the resource mapped by
     * this document producer. This stream is not guaranteed to be 
     * buffered.
     */
    Reader getStream(HttpServletRequest request) throws Exception;

    /**
     * This method is responsible to generate a DOM tree that
     * contains the generated data.
     */
    Document getDocument(HttpServletRequest request) throws Exception;
    
    /**
     * Returns the path where the resource is found, or an empty string if
     * no path can be applied to the resource.
     * Warning, null values are not valid.
     */
    String getPath(HttpServletRequest request);
    
}