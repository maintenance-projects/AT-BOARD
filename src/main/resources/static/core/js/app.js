(function () {
    function on(el, event, selector, handler) {
        el.addEventListener(event, function (e) {
            var t = e.target;
            while (t && t !== el) {
                if (t.matches(selector)) return handler.call(t, e);
                t = t.parentElement;
            }
        });
    }

    window.App = window.App || {};
    window.App.on = on;
})();
