package com.puckowski.launcher4;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

class ApplicationInfo 
{
    CharSequence title;
    Intent intent;
    Drawable icon;
    boolean filtered;
    String packageName;
    
    final void setActivity(ComponentName className, int launchFlags)
    {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        intent.setComponent(className);
        intent.setFlags(launchFlags);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ApplicationInfo))
        {
            return false;
        }

        ApplicationInfo that = (ApplicationInfo) o;
        
        return title.equals(that.title) &&
                intent.getComponent().getClassName().equals(
                        that.intent.getComponent().getClassName());
    }
}
