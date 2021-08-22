// IMxFrameworkService.aidl
package com.symbol.mxmf;

// Declare any non-default types here with import statements

interface IMxFrameworkService {
   /**
   	 * Provide Mx Framework Service(s) to process a clinet's request
   	 * @param  sRequest - request String in XML format sent by a client
   	 * @return a String from Mx Framework Service's response in XML format
   	 */
       String processXML(String sRequest);

      /**
   	 * Provide Mx Framework Service(s) to process a clinet's request
   	 * @param  sRequest - request String in XML format sent by a client
   	 * @param  mapExtra - a map that contains Extra information on how the request XML should be applied
   	 * @return a String from Mx Framework Service's response in XML format
   	 */
       String processXmlRequest(String sRequest, in Map mapExtra);

      /**
       	 * Get value from CSP by providing a key
       	 * @param  sKey - a key that CSP would understand, then return a value to MxFramework.
       	 * @return a value
       	 */
       String getValue(String sKey);

}
