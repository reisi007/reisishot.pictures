// Create a fake AMD environment, so that less libraries need fixes to work...
const define = (function () {
    let count = 0;
    const global = window.__reisishotFakeAmd = {};
    let unresolvedDependencies = [];

    const fakeAmd = function () {
        let method, internalName, publicName = undefined, paramNames;
        switch (arguments.length) {
            case 1:
                // A factory function without dependencies - cannot be built upon!
                method = arguments[0];
                internalName = calculateInternalName(undefined);
                paramNames = [];
                break;
            case 2:
                // A factory function with dependencies - cannot be built upon!
                paramNames = arguments[0];
                method = arguments[1];
                internalName = calculateInternalName(undefined);
                break;
            case 3:
                // A factory function with dependencies -  can be used to inject in pther modules!
                publicName = internalName = calculateInternalName(arguments[0]);
                paramNames = arguments[1];
                method = arguments[2];
                break;
            default:
                throw new Error("Unknown call... ")
        }

        // Queue for creation
        const creationRequest = {
            unmetDependencies: paramNames.filter(name => {
                if (name === 'exports')
                    return false;
                return global[name] == null
            }),
            creator: function () {
                global['exports'] = global[internalName] = {};
                const params = paramNames.map(exportName => global[exportName]);
                global[internalName] = method(...params);
                return publicName;
            }
        };
        if (creationRequest.unmetDependencies.length === 0)
            createModule(creationRequest.creator);
        else
            unresolvedDependencies.push(creationRequest);

    };

    function calculateInternalName(guess) {
        if (typeof guess === 'string')
            return guess;
        else {
            const ret = 'famd' + count;
            count++;
            return ret;
        }
    }

    function createModule(creator) {
        scanForUnusedModules(creator())
    }

    function scanForUnusedModules(moduleName) {
        if (moduleName)
            unresolvedDependencies.forEach((dependency, idx, array) => {
                dependency.unmetDependencies = dependency.unmetDependencies.filter(d => d !== moduleName);
                if (dependency.unmetDependencies.length === 0) {
                    array.splice(idx, 1);
                    createModule(dependency.creator);
                }
            })
    }

    fakeAmd.amd = true;
    return fakeAmd;
})();
