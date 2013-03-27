package de.cubeisland.cubeengine.signmarket;

import de.cubeisland.cubeengine.core.module.Module;
import de.cubeisland.cubeengine.core.permission.PermDefault;
import de.cubeisland.cubeengine.core.permission.Permission;
import de.cubeisland.cubeengine.core.permission.PermissionContainer;

import org.bukkit.permissions.Permissible;

import java.util.Locale;

import static de.cubeisland.cubeengine.core.permission.PermDefault.OP;

public class MarketSignPerm extends PermissionContainer
{
    public MarketSignPerm(Module module)
    {
        super(module);
        this.registerAllPermissions();
    }

    private static final Permission SIGNMARKET = Permission.BASE.createAbstractChild("signmarket");
    private static final Permission SIGN = SIGNMARKET.createAbstractChild("sign");

    private static final Permission SIGN_DESTROY = SIGN.createAbstractChild("destroy");
    public static final Permission SIGN_DESTROY_OWN = SIGN_DESTROY.createChild("own");
    public static final Permission SIGN_DESTROY_ADMIN = SIGN_DESTROY.createChild("admin");
    public static final Permission SIGN_DESTROY_OTHER = SIGN_DESTROY.createChild("other");

    private static final Permission SIGN_INVENTORY = SIGN.createAbstractChild("inventory");
    public static final Permission SIGN_INVENTORY_SHOW = SIGN_INVENTORY.createChild("show");
    public static final Permission SIGN_INVENTORY_ACCESS_OTHER = SIGN_DESTROY.createChild("access.other");

    public static final Permission SIGN_CREATE = SIGN.createChild("create");

    public static final Permission SIGN_CREATE_USER = SIGN_CREATE.createChild("user");
    public static final Permission SIGN_CREATE_USER_OTHER = SIGN_CREATE_USER.createChild("other");
    public static final Permission SIGN_CREATE_USER_BUY = SIGN_CREATE_USER.createChild("buy");
    public static final Permission SIGN_CREATE_USER_SELL = SIGN_CREATE_USER.createChild("sell");

    public static final Permission SIGN_CREATE_ADMIN = SIGN_CREATE.createChild("admin");
    public static final Permission SIGN_CREATE_ADMIN_BUY = SIGN_CREATE_ADMIN.createChild("buy");
    public static final Permission SIGN_CREATE_ADMIN_SELL = SIGN_CREATE_ADMIN.createChild("sell");
    public static final Permission SIGN_CREATE_ADMIN_STOCK = SIGN_CREATE_ADMIN.createChild("stock");
    public static final Permission SIGN_CREATE_ADMIN_NOSTOCK = SIGN_CREATE_ADMIN.createChild("nostock");

    public static final Permission SIGN_EDIT = SIGN.createChild("edit");
    public static final Permission SIGN_SETSTOCK = SIGN.createChild("setstock");
    public static final Permission SIGN_SIZE_CHANGE = SIGN.createChild("size.change");
    public static final Permission SIGN_SIZE_CHANGE_INFINITE = SIGN_SIZE_CHANGE.createChild("infinite");
}
