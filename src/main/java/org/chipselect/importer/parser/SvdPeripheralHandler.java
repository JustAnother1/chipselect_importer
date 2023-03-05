package org.chipselect.importer.parser;

import java.util.List;

import org.chipselect.importer.Tool;
import org.chipselect.importer.server.Request;
import org.chipselect.importer.server.Response;
import org.chipselect.importer.server.Server;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvdPeripheralHandler
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private final Server srv;
    private final SvdAddressBlockHandler addressBlockHandler;
    private final SvdInterruptHandler interruptHandler;
    private final SvdRegisterHandler registerHandler;
    private Response srvAllPeripherals = null;
    private int srvDeviceId = 0;

    public SvdPeripheralHandler(Server srv)
    {
        this.srv = srv;
        addressBlockHandler = new SvdAddressBlockHandler(srv);
        interruptHandler = new SvdInterruptHandler(srv);
        registerHandler = new SvdRegisterHandler(srv);
    }

    public void setDefaultSize(String default_size)
    {
    	if(null != default_size)
    	{
    		addressBlockHandler.setDefaultSize(default_size);
    		int size = Integer.decode(default_size);
    		registerHandler.setDefaultSize(size);
    	}
    	// else -> why bother with null?
    }

    public void setDefaultAccess(String default_access)
    {
        registerHandler.setDefaultAccess(default_access);
    }

    public void setDefaultResetValue(String default_resetValue)
    {
        registerHandler.setDefaultResetValue(default_resetValue);
    }

    public void setDefaultResetMask(String default_resetMask)
    {
        registerHandler.setDefaultResetMask(default_resetMask);
    }

    public void setDefaultProtection(String default_protection)
    {
        addressBlockHandler.setDefaultProtection(default_protection);
    }

    /**
     *
     * @param peripheral may not be a derived peripheral
     * @return true = success, false = error
     */
    public boolean handle(Element peripheral)
    {
        String name = peripheral.getChildText("name");
        log.trace("Peripheral: {}", name);
        // check if format is valid
        if(false == checkIfValidPeripheral(peripheral))
        {
            return false;
        }
        int srvIdx = getPeripheralSrvIndexFor(name);
        if(0 > srvIdx)
        {
            // new peripheral
            log.info("creating new peripheral {}", name);
            return createPeripheralInstanceFrom(peripheral);
        }
        else
        {
            // this peripheral is by definition not derived !
            log.info("updating peripheral ", name);
            return updateIndependentPeripheral(srvIdx, peripheral);
        }
    }

    /**
     *
     * @param svdDerivedPeripheral the derived peripheral
     * @param svdOriginalPeripheral the peripheral that it was derived from
     * @return true = success, false = error
     */
    public boolean handleDerived(Element svdDerivedPeripheral, Element svdOriginalPeripheral)
    {
        String name = svdDerivedPeripheral.getChildText("name");
        log.trace("Peripheral: {}", name);
        int srvIdx = getPeripheralSrvIndexFor(name);
        if(0 > srvIdx)
        {
            // new peripheral
            log.trace("creating new derived peripheral {}", name);
            log.info("creating new derived peripheral {}", name);
            return createPeripheralInstanceFromDerived(svdDerivedPeripheral, svdOriginalPeripheral);
        }
        else
        {
            // check if format is valid
            if(false == checkIfValidPeripheral(svdDerivedPeripheral))
            {
                return false;
            }
            // this peripheral is derived by definition !
            log.info("updating derived peripheral {}", name);
            return updateDerivedPeripheral(srvIdx, svdDerivedPeripheral, svdOriginalPeripheral);
        }
    }

    public boolean getAllPeripheralInstancesFromServer(int srvDeviceId)
    {
        if(0 == srvDeviceId)
        {
            srvAllPeripherals = null;
            log.error("no device id given");
            return false;
        }
        this.srvDeviceId = srvDeviceId;
        if(0 == srvDeviceId)
        {
            log.error("Device ID invalid !");
            return false;
        }
        Request req = new Request("peripheral_instance", Request.GET);
        req.addPostParameter("dev_id", srvDeviceId);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            srvAllPeripherals = null;
            log.error("could not read the peripherals from the server");
            return false;
        }
        srvAllPeripherals = res;
        return true;
    }

    private Response getPeripheralFromServer(int peripheralId)
    {
        if(0 == peripheralId)
        {
            log.error("Peripheral ID invalid !");
            return null;
        }
        Request req = new Request("peripheral", Request.GET);
        req.addPostParameter("id", peripheralId);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not read the fields from the server");
            return null;
        }
        else
        {
            return res;
        }
    }

    private int getPeripheralSrvIndexFor(String name)
    {
        for(int i = 0; i < srvAllPeripherals.numResults(); i++)
        {
            String svdName = srvAllPeripherals.getString(i, "name");
            if(false == svdName.equals(name))
            {
                // log.trace("Name mismatch: {} - {}", svdName, name);
                continue;
            }
            else
            {
                return i;
            }
        }
        // not found
        return -1;
    }

    private boolean checkIfValidPeripheral(Element peripheral)
    {
        // check for unknown children
        List<Element> children = peripheral.getChildren();
        for(Element child : children)
        {
            String name = child.getName();
            switch(name)
            {
            // all defined child types from SVD standard
            // compare to: https://arm-software.github.io/CMSIS_5/develop/SVD/html/elem_device.html
            case "access":
            case "addressBlock":
            case "alternatePeripheral":
            case "appendToName":
            case "baseAddress":
            case "description":
            case "dim":
            case "dimIncrement":
            case "dimIndex":
            case "dimName":
            case "dimArrayIndex":
            case "disableCondition":
            case "groupName":
            case "headerStructName":
            case "interrupt":
            case "name":
            case "prependToName":
            case "protection":
            case "registers":
            case "resetValue":
            case "resetMask":
            case "size":
            case "version":
                continue;

            default:
                // undefined child found. This is not a valid SVD file !
                log.error("Unknown peripheral child tag: {}", name);
                return false;
            }
        }
        return true;
    }

    private boolean updateIndependentPeripheral(int idx, Element peripheral)
    {
        // name - already handled
        // ignoring  <version></version>

        if(null !=  peripheral.getChildText("dim"))
        {
            log.error("dim not implemented!(={})", peripheral.getChildText("dim"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimIncrement"))
        {
            log.error("dimIncrement not implemented!(={})", peripheral.getChildText("dimIncrement"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimIndex"))
        {
            log.error("dimIndex not implemented!(={})", peripheral.getChildText("dimIndex"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimName"))
        {
            log.error("dimName not implemented!(={})", peripheral.getChildText("dimName"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimArrayIndex"))
        {
            log.error("dimArrayIndex not implemented!(={})",peripheral.getChildText("dimArrayIndex") );
            return false;
        }
        /* only available if address blocks of this peripheral are also used by another peripheral.
         * So basically only a warning that the address conflict is intentional.
         -> no need to store this.
        if(null !=  peripheral.getChildText("alternatePeripheral"))
        {
            log.error("alternatePeripheral not implemented!(={})");
            return false;
        }*/

        /* used by TI to give all UART0 Registers the prefix UART0,...
        we already know if a register named CR is part of UART0 or UART1.
       -> No need to store this! Enjoy shorter names ! ;-)
        if(null !=  peripheral.getChildText("prependToName"))
        {
            log.error("prependToName not implemented!(={})", peripheral.getChildText("prependToName"));
            return false;
        } */
        if(null !=  peripheral.getChildText("appendToName"))
        {
            log.error("appendToName not implemented!(={})",  peripheral.getChildText("appendToName"));
            return false;
        }
        /* used by Cmsemicon
         * -> ignore for now
        if(null !=  peripheral.getChildText("headerStructName"))
        {
            log.error("headerStructName not implemented!(={})", peripheral.getChildText("headerStructName"));
            return false;
        }*/

        // description
        String svdDescriptionValue = peripheral.getChildText("description");
        if(null == svdDescriptionValue)
        {
            // value not present -> OK
        }
        else
        {
            svdDescriptionValue = Tool.cleanupString(svdDescriptionValue);
            String srvValue = srvAllPeripherals.getString(idx, "description");
            if(false == svdDescriptionValue.equals(srvValue))
            {
                log.debug("update needed for {} from :{}: to :{}:!", "description", srvValue, svdDescriptionValue);

                int id = srvAllPeripherals.getInt(idx, "id");
                if(0 == id)
                {
                    log.error("Peripheral ID on server is invalid !");
                    return false;
                }

                if(false == updateServerPeripheralInstance(
                        id,
                        null,
                        svdDescriptionValue,
                        null,
                        0,
                        null ))
                {
                    log.error("update description failed!");
                    return false;
                }
            }
            // else value is the same so no update needed -> OK
        }

        // disableCondition
        // String checkIfUpdateNeeded(int idx, int origIdx, Element peripheral, String svdName, String serverName)
        String svdValue = peripheral.getChildText("disableCondition");
        if(null == svdValue)
        {
            // value not present -> OK
        }
        else
        {
            svdValue = Tool.cleanupString(svdValue);
            String srvValue = srvAllPeripherals.getString(idx, "disable_Condition");
            if(false == svdValue.equals(srvValue))
            {
                log.debug("update needed for {} from :{}: to :{}:!", "disableCondition", srvValue, svdValue);
                int id = srvAllPeripherals.getInt(idx, "id");
                if(0 == id)
                {
                    log.error("Peripheral ID on server is invalid !");
                    return false;
                }

                if(false == updateServerPeripheralInstance(
                        id,
                        null,
                        null,
                        null,
                        0,
                        svdValue ))
                {
                    log.error("update disableCondition failed!");
                    return false;
                }
            }
            // else value is the same so no update needed -> OK
        }

        // baseAddress
        HexString strBaseAddress = new HexString(peripheral.getChildText("baseAddress"));
        HexString strSrvBaseAddress = new HexString(srvAllPeripherals.getString(idx, "base_address"));
        if(false == strBaseAddress.equals(strSrvBaseAddress))
        {
            if(null != strBaseAddress.toString())
            {
                log.debug("update needed for {} from :{}: to :{}:!", "baseAddress", strSrvBaseAddress, strBaseAddress);
                log.error("update baseAddress not implemented!");
                return false;
            }
        }

        // peripheral
        int peripheralInstanceId = srvAllPeripherals.getInt(idx, "id");
        int peripheralId = srvAllPeripherals.getInt(idx, "peripheral_id");
        if(0 == peripheralInstanceId)
        {
            // peripheral not on server -> create new peripheral
            return createPeripheralInstanceFrom(peripheral);
        }
        Response srvPeripheral = getPeripheralFromServer(peripheralId);
        if(null == srvPeripheral)
        {
            log.error("could not read peripheral information from server!");
            return false;
        }
        // groupName
        String svdGroupName = peripheral.getChildText("groupName");
        if(null != svdGroupName)
        {
            if(0 < svdGroupName.length())
            {
                String srvGroupName = srvPeripheral.getString("group_name");
                if(false == svdGroupName.equals(srvGroupName))
                {
                    log.debug("group name changed from :{}: to :{}: !", srvGroupName, svdGroupName);
                    log.error("update group name - not implemented!");
                    return false;
                }
            }
        }

        // size
        String svdSize = peripheral.getChildText("size");
        if(null != svdSize)
        {
            setDefaultSize(svdSize);
        }
        // access
        String svdAccess = peripheral.getChildText("access");
        if(null != svdAccess)
        {
            setDefaultAccess(svdAccess);
        }
        // protection
        String svdProtection = peripheral.getChildText("protection");
        if(null != svdProtection)
        {
            setDefaultProtection(svdProtection);
        }
        // resetValue
        String svdResetValue = peripheral.getChildText("resetValue");
        if(null != svdResetValue)
        {
            setDefaultResetValue(svdResetValue);
        }
        // resetMask
        String svdResetMask = peripheral.getChildText("resetMask");
        if(null != svdResetMask)
        {
            setDefaultResetMask(svdResetMask);
        }

        // addressBlock
        if(false == addressBlockHandler.updateAddressBlock(peripheral, peripheralId))
        {
            return false;
        }

        // interrupt
        if(false == interruptHandler.updateInterrupt(peripheral, peripheralInstanceId))
        {
            return false;
        }

        // registers
        if(false == registerHandler.updateRegister(peripheral, peripheralId))
        {
            return false;
        }

        // all done
        return true;
    }

    private boolean updateDerivedPeripheral(int srvIdx, Element svdDerivedPeripheral, Element svdOriginalPeripheral)
    {
        // name - already handled
        // ignoring  <version></version>
        String OriginalName = svdOriginalPeripheral.getChildText("name");
        int OriginalsrvIdx = getPeripheralSrvIndexFor(OriginalName);
        if(0 > OriginalsrvIdx)
        {
            log.error("Server does not have the original peripheral ({})!", OriginalName);
            return false;
        }

        if(null !=  svdDerivedPeripheral.getChildText("dim"))
        {
            log.error("dim not implemented!(={})", svdDerivedPeripheral.getChildText("dim"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimIncrement"))
        {
            log.error("dimIncrement not implemented!(={})", svdDerivedPeripheral.getChildText("dimIncrement"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimIndex"))
        {
            log.error("dimIndex not implemented!(={})", svdDerivedPeripheral.getChildText("dimIndex"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimName"))
        {
            log.error("dimName not implemented!(={})", svdDerivedPeripheral.getChildText("dimName"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimArrayIndex"))
        {
            log.error("dimArrayIndex not implemented!(={})", svdDerivedPeripheral.getChildText("dimArrayIndex"));
            return false;
        }
        /* only available if address blocks of this peripheral are also used by another peripheral.
         * So basically only a warning that the address conflict is intentional.
         -> no need to store this.
        if(null !=  svdDerivedPeripheral.getChildText("alternatePeripheral"))
        {
            log.error("alternatePeripheral not implemented!(={})", svdDerivedPeripheral.getChildText("alternatePeripheral"));
            return false;
        }
        */

        /* used by TI to give all UART0 Registers the prefix UART0,...
        we already know if a register named CR is part of UART0 or UART1.
       -> No need to store this! Enjoy shorter names ! ;-)
        if(null !=  svdDerivedPeripheral.getChildText("prependToName"))
        {
            log.error("prependToName not implemented!(={})", svdDerivedPeripheral.getChildText("prependToName"));
            return false;
        } */
        if(null !=  svdDerivedPeripheral.getChildText("appendToName"))
        {
            log.error("appendToName not implemented!(={})", svdDerivedPeripheral.getChildText("appendToName"));
            return false;
        }
        /*  used by Cmsemicon
         * -> ignore for now
        if(null !=  svdDerivedPeripheral.getChildText("headerStructName"))
        {
            log.error("headerStructName not implemented!(={})", svdDerivedPeripheral.getChildText("headerStructName"));
            return false;
        }*/

        // description
        String svdDescriptionValue = svdDerivedPeripheral.getChildText("description");
        if(null == svdDescriptionValue)
        {
            svdDescriptionValue = svdOriginalPeripheral.getChildText("description");
        }
        if(null == svdDescriptionValue)
        {
            // value not present -> OK
        }
        else
        {
            svdDescriptionValue = Tool.cleanupString(svdDescriptionValue);
            String srvValue = srvAllPeripherals.getString(srvIdx, "description");
            srvValue = Tool.cleanupString(srvValue);
            if(false == svdDescriptionValue.equals(srvValue))
            {
                log.debug("update needed for {} from :{}: to :{}:!", "description", srvValue, svdDescriptionValue);

                int id = srvAllPeripherals.getInt(srvIdx, "id");
                if(0 == id)
                {
                    log.error("Peripheral ID on server is invalid !");
                    return false;
                }

                if(false == updateServerPeripheralInstance(
                        id,
                        null,
                        svdDescriptionValue,
                        null,
                        0,
                        null ))
                {
                    log.error("update description failed!");
                    return false;
                }
            }
            // else value is the same so no update needed -> OK
        }

        // disableCondition
        // private String checkIfUpdateNeeded(int idx, int origIdx, Element peripheral, String svdName, String serverName)
        String svdValue = svdDerivedPeripheral.getChildText("disableCondition");
        if(null == svdValue)
        {
            svdValue = svdOriginalPeripheral.getChildText("disableCondition");
        }
        if(null == svdValue)
        {
            // value not present -> OK
        }
        else
        {
            svdValue = Tool.cleanupString(svdValue);
            String srvValue = srvAllPeripherals.getString(srvIdx, "disable_Condition");
            if(false == svdValue.equals(srvValue))
            {
                log.debug("update needed for {} from :{}: to :{}:!", "disableCondition", srvValue, svdValue);
                int id = srvAllPeripherals.getInt(srvIdx, "id");
                if(0 == id)
                {
                    log.error("Peripheral ID on server is invalid !");
                    return false;
                }

                if(false == updateServerPeripheralInstance(
                        id,
                        null,
                        null,
                        null,
                        0,
                        svdValue ))
                {
                    log.error("update disableCondition failed!");
                    return false;
                }
            }
            // else value is the same so no update needed -> OK
        }

        // baseAddress
        HexString strBaseAddress = new HexString(svdDerivedPeripheral.getChildText("baseAddress"));
        HexString strSrvBaseAddress =  new HexString(srvAllPeripherals.getString(srvIdx, "base_address"));
        if(false == strBaseAddress.equals(strSrvBaseAddress))
        {
            if(null != strBaseAddress.toString())
            {
                log.debug("update needed for {} from :{}: to :{}:!", "baseAddress", strSrvBaseAddress, strBaseAddress);
                log.error("update baseAddress not implemented!");
                return false;
            }
        }

        // peripheral
        int peripheralId = srvAllPeripherals.getInt(srvIdx, "peripheral_id");
        if(0 == peripheralId)
        {
            peripheralId = srvAllPeripherals.getInt(OriginalsrvIdx, "peripheral_id");
        }
        if(0 == peripheralId)
        {
            // peripheral not on server -> create new peripheral
            log.error(srvAllPeripherals.dump(srvIdx));
            log.error(srvAllPeripherals.dump(OriginalsrvIdx));
            log.error("no peripheral - not implemented!");
            return false;
        }
        Response srvPeripheral = getPeripheralFromServer(peripheralId);
        if(null == srvPeripheral)
        {
            log.error("could not read peripheral information from server!");
            return false;
        }

        // groupName
        String svdGroupName = svdDerivedPeripheral.getChildText("groupName");
        if(null == svdGroupName)
        {
            svdGroupName = svdOriginalPeripheral.getChildText("groupName");
        }
        if(null != svdGroupName)
        {
            if(1 > svdGroupName.length())
            {
                svdGroupName = svdOriginalPeripheral.getChildText("groupName");
            }
            if(0 < svdGroupName.length())
            {
                String srvGroupName = srvPeripheral.getString("group_name");
                if(false == svdGroupName.equals(srvGroupName))
                {
                    log.debug("group name changed from :{}: to :{}: !", srvGroupName, svdGroupName);
                    log.error("update group name - not implemented!");
                    return false;
                }
                // else matches -> no change necessary
            }
            // else no group name given -> OK
        }
        // else no group name given -> OK

        // size
        String svdSize = svdDerivedPeripheral.getChildText("size");
        if(null != svdSize)
        {
            setDefaultSize(svdSize);
        }
        // access
        String svdAccess = svdDerivedPeripheral.getChildText("access");
        if(null != svdAccess)
        {
            setDefaultAccess(svdAccess);
        }
        // protection
        String svdProtection = svdDerivedPeripheral.getChildText("protection");
        if(null != svdProtection)
        {
            setDefaultProtection(svdProtection);
        }
        // resetValue
        String svdResetValue = svdDerivedPeripheral.getChildText("resetValue");
        if(null != svdResetValue)
        {
            setDefaultResetValue(svdResetValue);
        }
        // resetMask
        String svdResetMask = svdDerivedPeripheral.getChildText("resetMask");
        if(null != svdResetMask)
        {
            setDefaultResetMask(svdResetMask);
        }

        // "per_in_id" == srvIdx
        // peripheralId = peripheralId
        // addressBlock
        if(false == addressBlockHandler.updateDerivedAddressBlock(svdDerivedPeripheral, svdOriginalPeripheral, peripheralId)) // peripheral
        {
            return false;
        }

        // interrupt
        if(false == interruptHandler.updateDerivedInterrupt(svdDerivedPeripheral, svdOriginalPeripheral, srvIdx))// peripheralInstance
        {
            return false;
        }

        // registers
        if(false == registerHandler.updateDerivedRegister(svdDerivedPeripheral, svdOriginalPeripheral, peripheralId))// peripheral
        {
            return false;
        }

        // all done
        return true;
    }

    private boolean createPeripheralInstanceFrom(Element peripheral)
    {
        // name
        String svdName = peripheral.getChildText("name");
        log.trace("creating new independend peripheral for {}", svdName);
        // ignoring  <version></version>

        if(null !=  peripheral.getChildText("dim"))
        {
            log.error("dim not implemented!(={})", peripheral.getChildText("dim"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimIncrement"))
        {
            log.error("dimIncrement not implemented!(={})", peripheral.getChildText("dimIncrement"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimIndex"))
        {
            log.error("dimIndex not implemented!(={})", peripheral.getChildText("dimIndex"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimName"))
        {
            log.error("dimName not implemented!(={})", peripheral.getChildText("dimName"));
            return false;
        }
        if(null !=  peripheral.getChildText("dimArrayIndex"))
        {
            log.error("dimArrayIndex not implemented!(={})", peripheral.getChildText("dimArrayIndex"));
            return false;
        }
        /* only available if address blocks of this peripheral are also used by another peripheral.
         * So basically only a warning that the address conflict is intentional.
         -> no need to store this.
        if(null !=  peripheral.getChildText("alternatePeripheral"))
        {
            log.error("alternatePeripheral not implemented!(={})", peripheral.getChildText("alternatePeripheral"));
            return false;
        }*/

        /* used by TI to give all UART0 Registers the prefix UART0,...
          we already know if a register named CR is part of UART0 or UART1.
         -> No need to store this! Enjoy shorter names ! ;-)
        if(null !=  peripheral.getChildText("prependToName"))
        {
            log.error("prependToName not implemented!(={})",  peripheral.getChildText("prependToName"));
            return false;
        }*/
        if(null !=  peripheral.getChildText("appendToName"))
        {
            log.error("appendToName not implemented!(={})", peripheral.getChildText("appendToName"));
            return false;
        }
        /*  used by Cmsemicon
         * -> ignore for now
        if(null !=  peripheral.getChildText("headerStructName"))
        {
            log.error("headerStructName not implemented!(={})", peripheral.getChildText("headerStructName"));
            return false;
        }*/

        // description
        String svdDescriptionValue = peripheral.getChildText("description");
        if(null == svdDescriptionValue)
        {
            // value not present -> OK
        }
        else
        {
            svdDescriptionValue = Tool.cleanupString(svdDescriptionValue);
        }

        // disableCondition
        // String checkIfUpdateNeeded(int idx, int origIdx, Element peripheral, String svdName, String serverName)
        String svdDisableCondition = peripheral.getChildText("disableCondition");
        if(null == svdDisableCondition)
        {
            // value not present -> OK
        }
        else
        {
            svdDisableCondition = Tool.cleanupString(svdDisableCondition);
        }

        // baseAddress
        String strBaseAddress = peripheral.getChildText("baseAddress");

        // groupName
        String svdGroupName = peripheral.getChildText("groupName");

        // size
        String svdSize = peripheral.getChildText("size");
        if(null != svdSize)
        {
            setDefaultSize(svdSize);
        }
        // access
        String svdAccess = peripheral.getChildText("access");
        if(null != svdAccess)
        {
            setDefaultAccess(svdAccess);
        }
        // protection
        String svdProtection = peripheral.getChildText("protection");
        if(null != svdProtection)
        {
            setDefaultProtection(svdProtection);
        }
        // resetValue
        String svdResetValue = peripheral.getChildText("resetValue");
        if(null != svdResetValue)
        {
            setDefaultResetValue(svdResetValue);
        }
        // resetMask
        String svdResetMask = peripheral.getChildText("resetMask");
        if(null != svdResetMask)
        {
            setDefaultResetMask(svdResetMask);
        }

        // now all data is available so generate the peripheral Instance.
        int peripheralInstanceId =  postNewPeripheralInstanceToServer(
                svdName,// name,
                svdDescriptionValue, // description,
                strBaseAddress, // base_address,
                0, // peripheral_id,
                svdDisableCondition// disable_condition
                );
        if(0 == peripheralInstanceId)
        {
            // post failed :-(
            log.error("could not create new peripheral Instance on the server!");
            return false;
        }
        // as this is independent also create a new peripheral
        if(null == svdGroupName)
        {
            svdGroupName = svdName; // a group of one ;-)
        }
        int peripheralId =  postNewPeripheralToServer(svdGroupName);
        if(0 == peripheralId)
        {
            // post failed :-(
            log.error("could not create new peripheral on the server!");
            return false;
        }
        if(false == updateServerPeripheralInstance(
                peripheralInstanceId, // id
                null,  // name
                null, // description
                null, //base_address
                peripheralId,
                null // disable_condition
                ))
        {
            log.error("could not add the peripherl_id to the newly created peripheral_instance!");
            return false;
        }


        // addressBlock
        if(false == addressBlockHandler.updateAddressBlock(peripheral, peripheralId))
        {
            return false;
        }

        // interrupt
        if(false == interruptHandler.updateInterrupt(peripheral, peripheralInstanceId))
        {
            return false;
        }

        // registers
        if(false == registerHandler.updateRegister(peripheral, peripheralId))
        {
            return false;
        }

        // we might need this peripheral if some other is derived from it -> update the list of peripherals from the server
        if(false == getAllPeripheralInstancesFromServer(srvDeviceId))
        {
            log.error("Could not read device peripherals from sever");
            return false;
        }
        // all done
        return true;

    }

    private int postNewPeripheralToServer(String group_name)
    {
        if(null == group_name)
        {
            log.warn("group name is NULL !");
            return 0;
        }
        if(1 > group_name.length())
        {
            log.warn("group name is empty !");
            return 0;
        }
        Request req = new Request("peripheral", Request.POST);
        req.addPostParameter("group_name", group_name);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            return 0;
        }
        else
        {
            return res.getInt("id");
        }
    }

    private int postNewPeripheralInstanceToServer(
            String name,
            String description,
            String base_address,
            int peripheral_id,
            String disable_condition )
    {
        Request req = new Request("peripheral_instance", Request.POST);
        if(null != name)
        {
            req.addPostParameter("name", name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        if(null != base_address)
        {
            req.addPostParameter("base_address", base_address);
        }
        req.addPostParameter("peripheral_id", peripheral_id);
        if(null != disable_condition)
        {
            req.addPostParameter("disable_condition", disable_condition);
        }
        req.addPostParameter("dev_id", srvDeviceId);
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            return 0;
        }
        else
        {
            return res.getInt("id");
        }
    }

    private boolean updateServerPeripheralInstance(
            int id,
            String name,
            String description,
            String base_address,
            int peripheral_id,
            String disable_Condition )
    {
        Request req = new Request("peripheral_instance", Request.PUT);
        req.addPostParameter("id", id);
        if(null != name)
        {
            req.addPostParameter("name", name);
        }
        if(null != description)
        {
            req.addPostParameter("description", description);
        }
        if(null != base_address)
        {
            req.addPostParameter("base_address", base_address);
        }
        if(0 != peripheral_id)
        {
            req.addPostParameter("peripheral_id", peripheral_id);
        }
        if(null != disable_Condition)
        {
            req.addPostParameter("disable_Condition", disable_Condition);
        }
        Response res = srv.execute(req);
        if(false == res.wasSuccessfull())
        {
            log.error("could not update the peripheral instance on the server");
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean createPeripheralInstanceFromDerived(Element svdDerivedPeripheral, Element svdOriginalPeripheral)
    {
        // name
        String svdName = svdDerivedPeripheral.getChildText("name");
        // ignoring  <version></version>

        if(null !=  svdDerivedPeripheral.getChildText("dim"))
        {
            log.error("dim not implemented!(={})", svdDerivedPeripheral.getChildText("dim"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimIncrement"))
        {
            log.error("dimIncrement not implemented!(={})", svdDerivedPeripheral.getChildText("dimIncrement"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimIndex"))
        {
            log.error("dimIndex not implemented!(={})", svdDerivedPeripheral.getChildText("dimIndex"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimName"))
        {
            log.error("dimName not implemented!(={})", svdDerivedPeripheral.getChildText("dimName"));
            return false;
        }
        if(null !=  svdDerivedPeripheral.getChildText("dimArrayIndex"))
        {
            log.error("dimArrayIndex not implemented!(={})", svdDerivedPeripheral.getChildText("dimArrayIndex"));
            return false;
        }
        /* only available if address blocks of this peripheral are also used by another peripheral.
         * So basically only a warning that the address conflict is intentional.
         -> no need to store this.
        if(null !=  svdDerivedPeripheral.getChildText("alternatePeripheral"))
        {
            log.error("alternatePeripheral not implemented!(={})", svdDerivedPeripheral.getChildText("alternatePeripheral"));
            return false;
        }*/

        /* used by TI to give all UART0 Registers the prefix UART0,...
           we already know if a register named CR is part of UART0 or UART1.
           -> No need to store this! Enjoy shorter names ! ;-)
        if(null !=  peripheral.getChildText("prependToName"))
        {
            log.error("prependToName not implemented!(={})", peripheral.getChildText("prependToName"));
            return false;
        }*/
        if(null !=  svdDerivedPeripheral.getChildText("appendToName"))
        {
            log.error("appendToName not implemented!(={})", svdDerivedPeripheral.getChildText("appendToName"));
            return false;
        }
        /* used by Cmsemicon
         * -> ignore for now
        if(null !=  svdDerivedPeripheral.getChildText("headerStructName"))
        {
            log.error("headerStructName not implemented!(={})", svdDerivedPeripheral.getChildText("headerStructName"));
            return false;
        }*/

        // description
        String svdDescriptionValue = svdDerivedPeripheral.getChildText("description");
        if(null == svdDescriptionValue)
        {
            svdDescriptionValue = svdOriginalPeripheral.getChildText("description");
        }
        if(null == svdDescriptionValue)
        {
            // value not present -> OK
        }
        else
        {
            svdDescriptionValue = Tool.cleanupString(svdDescriptionValue);
        }

        // disableCondition
        String svdDisableCondition = svdDerivedPeripheral.getChildText("disableCondition");
        if(null == svdDisableCondition)
        {
            svdDisableCondition = svdOriginalPeripheral.getChildText("disableCondition");
        }
        if(null == svdDisableCondition)
        {
            // value not present -> OK
        }
        else
        {
            svdDisableCondition = Tool.cleanupString(svdDisableCondition);
        }

        // baseAddress
        String strBaseAddress = svdDerivedPeripheral.getChildText("baseAddress");
        if(null == strBaseAddress)
        {
            strBaseAddress = svdOriginalPeripheral.getChildText("baseAddress");
        }

        // groupName
        String svdGroupName = svdDerivedPeripheral.getChildText("groupName");
        if(null == svdGroupName)
        {
            svdGroupName = svdOriginalPeripheral.getChildText("groupName");
        }

        // size
        String svdSize = svdDerivedPeripheral.getChildText("size");
        if(null != svdSize)
        {
            svdSize = svdOriginalPeripheral.getChildText("size");
        }
        if(null != svdSize)
        {
            setDefaultSize(svdSize);
        }

        // access
        String svdAccess = svdDerivedPeripheral.getChildText("access");
        if(null != svdAccess)
        {
            svdAccess = svdOriginalPeripheral.getChildText("access");
        }
        if(null != svdAccess)
        {
            setDefaultAccess(svdAccess);
        }

        // protection
        String svdProtection = svdDerivedPeripheral.getChildText("protection");
        if(null != svdProtection)
        {
            svdProtection = svdOriginalPeripheral.getChildText("protection");
        }
        if(null != svdProtection)
        {
            setDefaultProtection(svdProtection);
        }

        // resetValue
        String svdResetValue = svdDerivedPeripheral.getChildText("resetValue");
        if(null != svdResetValue)
        {
            svdResetValue = svdOriginalPeripheral.getChildText("resetValue");
        }
        if(null != svdResetValue)
        {
            setDefaultResetValue(svdResetValue);
        }

        // resetMask
        String svdResetMask = svdDerivedPeripheral.getChildText("resetMask");
        if(null != svdResetMask)
        {
            svdResetMask = svdOriginalPeripheral.getChildText("resetMask");
        }
        if(null != svdResetMask)
        {
            setDefaultResetMask(svdResetMask);
        }

        int peripheralId = 0;
        int srvOrigIdx = getPeripheralSrvIndexFor(svdOriginalPeripheral.getChildText("name"));
        if(0 > srvOrigIdx)
        {
            log.error("Server does not have the original peripheral {}!", svdOriginalPeripheral.getChildText("name"));
            log.error("Server has these peripherals: {}", srvAllPeripherals.dumpAllNames());
            return false;
        }
        peripheralId = srvAllPeripherals.getInt(srvOrigIdx, "peripheral_id");
        if(0 == peripheralId)
        {
            log.error("No Peripheral ID for {}", svdName);
            log.error(srvAllPeripherals.dump(srvOrigIdx));
            int srvDerivedIdx = getPeripheralSrvIndexFor(svdDerivedPeripheral.getChildText("name"));
            if(0 > srvDerivedIdx)
            {
                log.error("Server does not have the derived peripheral !");
                return false;
            }
            log.error(srvAllPeripherals.dump(srvDerivedIdx));
            return false;
        }

        // now all data is available so generate the peripheral Instance.
        int peripheralInstanceId =  postNewPeripheralInstanceToServer(
                svdName,// name,
                svdDescriptionValue, // description,
                strBaseAddress, // base_address,
                peripheralId, // peripheral_id,
                svdDisableCondition// disable_condition
                );
        if(0 == peripheralInstanceId)
        {
            // post failed :-(
            log.error("could not create new derived peripheral Instance on the server!");
            return false;
        }


        // addressBlock
        if(false == addressBlockHandler.updateDerivedAddressBlock(svdDerivedPeripheral, svdOriginalPeripheral, peripheralId))
        {
            return false;
        }

        // interrupt
        if(false == interruptHandler.updateDerivedInterrupt(svdDerivedPeripheral, svdOriginalPeripheral, peripheralInstanceId))
        {
            return false;
        }

        // registers
        if(false == registerHandler.updateDerivedRegister(svdDerivedPeripheral, svdOriginalPeripheral, peripheralId))
        {
            return false;
        }

        // we might need this peripheral if some other is derived from it -> update the list of peripherals from the server
        if(false == getAllPeripheralInstancesFromServer(srvDeviceId))
        {
            log.error("Could not read device peripherals from sever");
            return false;
        }

        // all done
        return true;

    }

}
