// Create a fake AMD environment, so that less libraries need fixes to work... Needs to be included first
const define = (function () {
    let unnamedCount = 0;
    const global = {};
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
        unresolvedDependencies.push({
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
        });

        scanForCreatableModules();
    };

    function calculateInternalName(guess) {
        if (typeof guess === 'string')
            return guess;
        else {
            const ret = 'famd' + unnamedCount;
            unnamedCount++;
            return ret;
        }
    }

    function createModule(creator) {
        const moduleName = creator();
        if (moduleName) {
            unresolvedDependencies.forEach((dependency, idx, array) => {
                    dependency.unmetDependencies = dependency.unmetDependencies.filter(d => d !== moduleName)
                }
            );
            scanForCreatableModules()
        }
    }

    function scanForCreatableModules() {
        unresolvedDependencies.forEach((dependency, idx, array) => {
            if (dependency.unmetDependencies.length === 0) {
                array.splice(idx, 1);
                createModule(dependency.creator);
            }
        });
    }

    fakeAmd.amd = true;
    fakeAmd.unresolved = unresolvedDependencies;
    return fakeAmd;
})();