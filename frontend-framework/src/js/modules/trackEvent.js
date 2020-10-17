define('trackAction', [], function () {
    const matomo = _paq;

    return function (input) {
        console.log("Tracking" + JSON.stringify(input))
        if (typeof input.value === 'undefined')
            matomo.push([
                'trackEvent',
                input.group,
                input.action,
                input.id,
                input.value
            ])
        else
            matomo.push([
                'trackEvent',
                input.group,
                input.action,
                input.id
            ])
    }
})