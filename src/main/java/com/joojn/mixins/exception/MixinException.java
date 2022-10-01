package com.joojn.mixins.exception;

public class MixinException extends Throwable {

    public MixinException(String err)
    {
        super(err);
    }

    public MixinException()
    {
        super();
    }

    public MixinException(String err, Object... args)
    {
        super(String.format(err, args));
    }
}
