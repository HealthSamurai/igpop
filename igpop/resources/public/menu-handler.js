$(document).ready(function(){
    rollUpMenu();
})

function rollUpMenu()
{
    let dropdown = document.getElementsByClassName("dropdown-btn");
    for (let i = 0; i < dropdown.length; i++) {
        let dropdownContent = dropdown[i].nextElementSibling;
        dropdownContent.style.display = "none";
    }
}

$(".btn-patient").click(function() {
    $(".body-content").load("/assets/patient.html");
    return false;
});

$(".btn-organisation").click(function() {
    $(".body-content").load("/assets/organization.html");
    return false;
});
