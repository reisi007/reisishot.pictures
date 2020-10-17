define('trackAction', [], function () {
    return function (input) {
        if (typeof input.value === 'undefined')
            _paq.push([
                'trackEvent',
                input.group,
                input.action,
                input.id,
                input.value
            ])
        else
            _paq.push([
                'trackEvent',
                input.group,
                input.action,
                input.id
            ])
    }
})