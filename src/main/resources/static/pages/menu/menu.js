(function () {
    function goBack() {
        if (history.length > 1) history.back();
        else location.href = "/";
    }

    document.addEventListener("click", function (e) {
        var back = e.target.closest('[data-action="go-back"]');
        if (back) {
            e.preventDefault();
            goBack();
            return;
        }

        var tile = e.target.closest(".tile");
        if (tile) {
            var route = tile.getAttribute("data-route");
            if (route) {
                e.preventDefault();
                location.href = route;
            }
        }
    });
})();
