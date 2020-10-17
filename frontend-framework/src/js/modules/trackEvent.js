define('trackAction', [], function () {
    return function (input) {
        const action = input.group + '_' + input.action;
        if (typeof input.value === 'undefined')
            _paq.push([
                'trackEvent',
                input.group,
                action,
                input.id,
                input.value
            ])
        else
            _paq.push([
                'trackEvent',
                input.group,
                action,
                input.id
            ])
    }
})